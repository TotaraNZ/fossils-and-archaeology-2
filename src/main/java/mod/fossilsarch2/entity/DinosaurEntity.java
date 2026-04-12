package mod.fossilsarch2.entity;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.ai.DinoEatFernGoal;
import mod.fossilsarch2.entity.ai.DinoUseFeederGoal;
import mod.fossilsarch2.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

public class DinosaurEntity extends TamableAnimal implements GeoEntity {

    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation BITE_ANIM = RawAnimation.begin().thenPlay("bite");
    protected static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("eat");
    protected static final RawAnimation SNIFF_ANIM = RawAnimation.begin().thenPlay("sniff");
    protected static final RawAnimation LOOK_ANIM = RawAnimation.begin().thenPlay("look");

    private static final String SPECIAL_CONTROLLER = "Special";
    private int specialAnimCooldown = 0;

    private static final EntityDataAccessor<Integer> DINO_AGE = SynchedEntityData.defineId(
            DinosaurEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(
            DinosaurEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(
            DinosaurEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> EATING = SynchedEntityData.defineId(
            DinosaurEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int DEFAULT_MAX_HUNGER = 100;

    // Bump on NBT schema changes; migrate in readAdditionalSaveData
    private static final int CURRENT_DATA_VERSION = 1;

    private final String dinosaurId;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DinosaurEntity(EntityType<? extends DinosaurEntity> type, Level level) {
        super(type, level);
        Identifier id = EntityType.getKey(type);
        this.dinosaurId = id != null ? id.getPath() : "";

        // registerGoals() is called from super() before dinosaurId is set,
        // so diet-dependent goals must be added here after construction
        setupDietGoals();

        // Assign random variant if species has them and none set
        Dinosaur dino = getDinosaur();
        if (dino != null && dino.variants != null && !dino.variants.isEmpty() && getVariant().isEmpty()) {
            setVariant(dino.variants.get(getRandom().nextInt(dino.variants.size())));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DINO_AGE, 0);
        builder.define(HUNGER, DEFAULT_MAX_HUNGER);
        builder.define(VARIANT, "");
        builder.define(EATING, false);
    }

    @Override
    protected void registerGoals() {
        // Only register diet-independent goals here (dinosaurId is null at this point)
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new DinoUseFeederGoal(this));
        this.goalSelector.addGoal(4, new DinoEatFernGoal(this));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1D, stack -> stack.getItem() == ModItems.FERN_SEED, false));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(7, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    private void setupDietGoals() {
        Dinosaur dino = getDinosaur();
        if (dino == null) {
            FossilsArch2Mod.LOGGER.warn("setupDietGoals: getDinosaur() returned null for dinosaurId='{}'", dinosaurId);
            return;
        }
        Dinosaur.Diet diet = dino.diet != null ? dino.diet : Dinosaur.Diet.HERBIVORE;

        if (diet == Dinosaur.Diet.CARNIVORE || diet == Dinosaur.Diet.OMNIVORE) {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, dino.chase_speed, false));
            // Hunt animals (not other dinosaurs)
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, false));
            // Wild (untamed) carnivores attack players on sight
            this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, false,
                    (target, level) -> !this.isTame()));
        }

        if (diet == Dinosaur.Diet.HERBIVORE) {
            this.goalSelector.addGoal(1, new PanicGoal(this, dino.flee_speed));
        }
    }

    public static AttributeSupplier.Builder createAttributes(Dinosaur d) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, d.health)
                .add(Attributes.MOVEMENT_SPEED, d.speed)
                .add(Attributes.ATTACK_DAMAGE, d.attack_damage)
                .add(Attributes.TEMPT_RANGE, 16.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // For registry fallback
    public static AttributeSupplier.Builder createMobAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.TEMPT_RANGE, 16.0);
    }

    // --- Dinosaur Data ---

    public String getDinosaurId() {
        return dinosaurId;
    }

    public Dinosaur getDinosaur() {
        if (dinosaurId == null || dinosaurId.isEmpty()) return null;
        // Look up by species ID across all namespaces (supports addon mods)
        return mod.fossilsarch2.dinosaur.DinosaurUtils.getBySpeciesId(dinosaurId);
    }

    public String getVariant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(String variant) {
        entityData.set(VARIANT, variant);
    }

    // --- Age / Growth ---

    public int getDinoAge() {
        return entityData.get(DINO_AGE);
    }

    public void setDinoAge(int age) {
        Dinosaur dino = getDinosaur();
        int clampedAge = dino != null ? Math.max(0, Math.min(age, dino.max_age)) : Math.max(0, age);

        if (entityData.get(DINO_AGE) == clampedAge) {
            return;
        }

        entityData.set(DINO_AGE, clampedAge);
        refreshDimensions();
    }

    public boolean growByStages(int stages) {
        Dinosaur dino = getDinosaur();
        if (dino == null || stages <= 0 || getDinoAge() >= dino.max_age) {
            return false;
        }

        setDinoAge(Math.min(getDinoAge() + stages, dino.max_age));
        return true;
    }

    public boolean isBaby() {
        Dinosaur dino = getDinosaur();
        return dino != null && getDinoAge() < dino.max_age;
    }

    @Override
    public float getAgeScale() {
        return getScaleFactor();
    }

    public float getScaleFactor() {
        Dinosaur dino = getDinosaur();
        if (dino == null) return 1.0f;
        if (getDinoAge() >= dino.max_age) return 1.0f;
        float progress = (float) getDinoAge() / dino.max_age;
        return dino.baby_scale + (1.0f - dino.baby_scale) * progress;
    }

    // --- Hunger ---

    public int getHunger() {
        return entityData.get(HUNGER);
    }

    public void setHunger(int hunger) {
        entityData.set(HUNGER, Math.max(0, Math.min(getMaxHunger(), hunger)));
    }

    public boolean isHungry() {
        Dinosaur dino = getDinosaur();
        int threshold = dino != null ? dino.hungry_threshold : 80;
        return getHunger() < threshold;
    }

    public int getMaxHunger() {
        Dinosaur dino = getDinosaur();
        return dino != null ? dino.max_hunger : DEFAULT_MAX_HUNGER;
    }

    public float getGrowthProgress() {
        Dinosaur dino = getDinosaur();
        if (dino == null || dino.max_age <= 0) return 1.0f;
        return (float) getDinoAge() / dino.max_age;
    }

    public void feed(int amount) {
        setHunger(getHunger() + amount);
    }

    public boolean isEating() {
        return entityData.get(EATING);
    }

    public void setEating(boolean eating) {
        entityData.set(EATING, eating);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (DINO_AGE.equals(key)) {
            refreshDimensions();
        }
    }

    // --- Taming ---

    public void setTamedBy(Player player) {
        tame(player);
        setTame(true, true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Dinopedia interaction
        if (stack.getItem() == ModItems.DINOPEDIA && !level().isClientSide()) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    (net.minecraft.server.level.ServerPlayer) player,
                    new mod.fossilsarch2.network.DinopediaPayload(getId()));
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() == ModItems.ESSENCE_CHICKEN) {
            if (!level().isClientSide()) {
                if (!isBaby()) {
                    player.sendSystemMessage(Component.translatable("message.fossilsarch2.essence_fail_adult"));
                    return InteractionResult.PASS;
                }

                if (growByStages(1)) {
                    if (!player.isCreative()) {
                        consumeHeldItemAndGiveRemainder(player, hand, stack, new ItemStack(Items.GLASS_BOTTLE));
                    }
                    playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), 0.8f, 1.0f);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Taming with food
        if (!isTame() && isDinoFood(stack)) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            if (!level().isClientSide()) {
                Dinosaur dinoData = getDinosaur();
                float tameChance = dinoData != null ? dinoData.tame_chance : 0.33f;
                if (getRandom().nextFloat() < tameChance) {
                    setTamedBy(player);
                    level().broadcastEntityEvent(this, (byte) 7); // hearts
                } else {
                    level().broadcastEntityEvent(this, (byte) 6); // smoke
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private boolean isDinoFood(ItemStack stack) {
        Dinosaur dino = getDinosaur();
        if (dino == null) return false;

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = itemId.getPath();

        if (dino.diet != Dinosaur.Diet.CARNIVORE) {
            if (stack.getItem() == ModItems.FERN_SEED) return true;
        }
        if (dino.diet != Dinosaur.Diet.HERBIVORE) {
            if (path.endsWith("_meat")) return true;
        }

        return false;
    }

    private static void consumeHeldItemAndGiveRemainder(Player player, InteractionHand hand, ItemStack heldStack, ItemStack remainder) {
        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, remainder);
        } else if (!player.getInventory().add(remainder)) {
            player.drop(remainder, false);
        }
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        Dinosaur dino = getDinosaur();
        if (dino == null) return;

        // Growth
        if (getDinoAge() < dino.max_age && tickCount % dino.grow_time == 0 && tickCount > 0) {
            setDinoAge(getDinoAge() + 1);
        }

        // Hunger decrease
        int decayRate = dino.hunger_decay_rate;
        if (decayRate > 0 && tickCount % decayRate == 0) {
            setHunger(getHunger() - 1);
        }

        // Starvation
        if (getHunger() <= 0 && tickCount % 40 == 0) {
            hurtServer((ServerLevel) level(), damageSources().starve(), 1.0f);
        }

        // Random special idle animations from JSON
        if (specialAnimCooldown > 0) {
            specialAnimCooldown--;
        } else if (dino.special_animations != null && !dino.special_animations.isEmpty()
                && getNavigation().isDone() && getTarget() == null && getRandom().nextInt(200) == 0) {
            String anim = dino.special_animations.get(getRandom().nextInt(dino.special_animations.size()));
            triggerAnim(SPECIAL_CONTROLLER, anim);
            specialAnimCooldown = 300;
        }
    }

    // --- Death drops ---

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        Dinosaur dino = getDinosaur();
        if (dino == null) return;

        net.minecraft.world.item.Item meatItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, dino.id + "_meat"));
        if (meatItem != net.minecraft.world.item.Items.AIR) {
            int count = 1 + getRandom().nextInt(dino.meat_drop_count);
            spawnAtLocation(level, new ItemStack(meatItem, count));
        }
    }

    // --- Sounds ---

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return mod.fossilsarch2.registry.ModSounds.get(dinosaurId + ".ambient");
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return mod.fossilsarch2.registry.ModSounds.get(dinosaurId + ".hurt");
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return mod.fossilsarch2.registry.ModSounds.get(dinosaurId + ".death");
    }

    // --- NBT ---

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("FA2DataVersion", CURRENT_DATA_VERSION);
        output.putInt("DinoAge", getDinoAge());
        output.putInt("Hunger", getHunger());
        output.putString("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int savedVersion = input.getIntOr("FA2DataVersion", 0);
        // if (savedVersion < 2) { ... }

        setDinoAge(input.getIntOr("DinoAge", 0));
        setHunger(input.getIntOr("Hunger", getMaxHunger()));
        setVariant(input.getStringOr("Variant", ""));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // Dinosaurs don't breed via vanilla mechanics
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return null;
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("Movement", 5, this::movementController));
        controllers.add(new AnimationController<>("Attack", 5, this::attackController));

        // Register species-specific special animations from JSON
        Dinosaur dino = getDinosaur();
        if (dino != null && dino.special_animations != null && !dino.special_animations.isEmpty()) {
            AnimationController<DinosaurEntity> special = new AnimationController<>(SPECIAL_CONTROLLER, 5,
                    animTest -> PlayState.STOP);
            for (String animName : dino.special_animations) {
                special.triggerableAnim(animName, RawAnimation.begin().thenPlay(animName));
            }
            controllers.add(special);
        }
    }

    protected PlayState movementController(final AnimationTest<DinosaurEntity> animTest) {
        if (animTest.isMoving())
            return animTest.setAndContinue(WALK_ANIM);

        // Play idle if the species has one, otherwise stop
        return animTest.setAndContinue(IDLE_ANIM);
    }

    protected PlayState attackController(final AnimationTest<DinosaurEntity> animTest) {
        if (this.swinging) {
            return animTest.setAndContinue(BITE_ANIM);
        }
        if (this.isEating()) {
            return animTest.setAndContinue(EAT_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
