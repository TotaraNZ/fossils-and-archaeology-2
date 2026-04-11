package mod.fossilsarch2.entity;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.ai.DinoEatFernGoal;
import mod.fossilsarch2.entity.ai.DinoUseFeederGoal;
import mod.fossilsarch2.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animatable.processing.AnimationTest;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DinosaurEntity extends TameableEntity implements GeoEntity {

    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation BITE_ANIM = RawAnimation.begin().thenPlay("bite");
    protected static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("eat");
    protected static final RawAnimation SNIFF_ANIM = RawAnimation.begin().thenPlay("sniff");
    protected static final RawAnimation LOOK_ANIM = RawAnimation.begin().thenPlay("look");

    private static final String SPECIAL_CONTROLLER = "Special";
    private int specialAnimCooldown = 0;

    private static final TrackedData<Integer> DINO_AGE = DataTracker.registerData(
            DinosaurEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> HUNGER = DataTracker.registerData(
            DinosaurEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> VARIANT = DataTracker.registerData(
            DinosaurEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> EATING = DataTracker.registerData(
            DinosaurEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final int DEFAULT_MAX_HUNGER = 100;

    // Bump on NBT schema changes; migrate in readCustomDataFromNbt
    private static final int CURRENT_DATA_VERSION = 1;

    private final String dinosaurId;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DinosaurEntity(EntityType<? extends DinosaurEntity> type, World world) {
        super(type, world);
        Identifier id = EntityType.getId(type);
        this.dinosaurId = id != null ? id.getPath() : "";

        // initGoals() is called from super() before dinosaurId is set,
        // so diet-dependent goals must be added here after construction
        setupDietGoals();

        // Assign random variant if species has them and none set
        Dinosaur dino = getDinosaur();
        if (dino != null && dino.variants != null && !dino.variants.isEmpty() && getVariant().isEmpty()) {
            setVariant(dino.variants.get(getRandom().nextInt(dino.variants.size())));
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DINO_AGE, 0);
        builder.add(HUNGER, DEFAULT_MAX_HUNGER);
        builder.add(VARIANT, "");
        builder.add(EATING, false);
    }

    @Override
    protected void initGoals() {
        // Only register diet-independent goals here (dinosaurId is null at this point)
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(3, new DinoUseFeederGoal(this));
        this.goalSelector.add(4, new DinoEatFernGoal(this));
        this.goalSelector.add(5, new TemptGoal(this, 1.1D, stack -> stack.isOf(ModItems.FERN_SEED), false));
        this.goalSelector.add(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.add(7, new FollowParentGoal(this, 1.0D));
        this.goalSelector.add(8, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(10, new LookAroundGoal(this));

        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
    }

    private void setupDietGoals() {
        Dinosaur dino = getDinosaur();
        if (dino == null) {
            FossilsArch2Mod.LOGGER.warn("setupDietGoals: getDinosaur() returned null for dinosaurId='{}'", dinosaurId);
            return;
        }
        Dinosaur.Diet diet = dino.diet != null ? dino.diet : Dinosaur.Diet.HERBIVORE;

        if (diet == Dinosaur.Diet.CARNIVORE || diet == Dinosaur.Diet.OMNIVORE) {
            this.goalSelector.add(2, new MeleeAttackGoal(this, dino.chase_speed, false));
            // Hunt animals (not other dinosaurs)
            this.targetSelector.add(4, new ActiveTargetGoal<>(this, AnimalEntity.class, false));
            // Wild (untamed) carnivores attack players on sight
            this.targetSelector.add(5, new ActiveTargetGoal<>(this, PlayerEntity.class, false,
                    (target, world) -> !this.isTamed()));
        }

        if (diet == Dinosaur.Diet.HERBIVORE) {
            this.goalSelector.add(1, new EscapeDangerGoal(this, dino.flee_speed));
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes(Dinosaur d) {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, d.health)
                .add(EntityAttributes.MOVEMENT_SPEED, d.speed)
                .add(EntityAttributes.ATTACK_DAMAGE, d.attack_damage)
                .add(EntityAttributes.TEMPT_RANGE, 16.0)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.STEP_HEIGHT, 1.0);
    }

    // For registry fallback
    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.TEMPT_RANGE, 16.0);
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
        return dataTracker.get(VARIANT);
    }

    public void setVariant(String variant) {
        dataTracker.set(VARIANT, variant);
    }

    // --- Age / Growth ---

    public int getDinoAge() {
        return dataTracker.get(DINO_AGE);
    }

    public void setDinoAge(int age) {
        dataTracker.set(DINO_AGE, age);
    }

    public boolean isBaby() {
        Dinosaur dino = getDinosaur();
        return dino != null && getDinoAge() < dino.max_age;
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
        return dataTracker.get(HUNGER);
    }

    public void setHunger(int hunger) {
        dataTracker.set(HUNGER, Math.max(0, Math.min(getMaxHunger(), hunger)));
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
        return dataTracker.get(EATING);
    }

    public void setEating(boolean eating) {
        dataTracker.set(EATING, eating);
    }

    // --- Taming ---

    public void setTamedBy(PlayerEntity player) {
        setOwner(player);
        setTamed(true, true);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Dinopedia interaction
        if (stack.isOf(ModItems.DINOPEDIA) && !getWorld().isClient()) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    (net.minecraft.server.network.ServerPlayerEntity) player,
                    new mod.fossilsarch2.network.DinopediaPayload(getId()));
            return ActionResult.SUCCESS;
        }

        // Taming with food
        if (!isTamed() && isFood(stack)) {
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            if (!getWorld().isClient()) {
                Dinosaur dinoData = getDinosaur();
                float tameChance = dinoData != null ? dinoData.tame_chance : 0.33f;
                if (getRandom().nextFloat() < tameChance) {
                    setTamedBy(player);
                    getWorld().sendEntityStatus(this, (byte) 7); // hearts
                } else {
                    getWorld().sendEntityStatus(this, (byte) 6); // smoke
                }
            }
            return ActionResult.SUCCESS;
        }

        return super.interactMob(player, hand);
    }

    private boolean isFood(ItemStack stack) {
        Dinosaur dino = getDinosaur();
        if (dino == null) return false;

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String path = itemId.getPath();

        if (dino.diet != Dinosaur.Diet.CARNIVORE) {
            if (stack.isOf(ModItems.FERN_SEED)) return true;
        }
        if (dino.diet != Dinosaur.Diet.HERBIVORE) {
            if (path.endsWith("_meat")) return true;
        }

        return false;
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) return;

        Dinosaur dino = getDinosaur();
        if (dino == null) return;

        // Growth
        if (getDinoAge() < dino.max_age && age % dino.grow_time == 0 && age > 0) {
            setDinoAge(getDinoAge() + 1);
        }

        // Hunger decrease
        int decayRate = dino.hunger_decay_rate;
        if (decayRate > 0 && age % decayRate == 0) {
            setHunger(getHunger() - 1);
        }

        // Starvation
        if (getHunger() <= 0 && age % 40 == 0) {
            damage((ServerWorld) getWorld(), getDamageSources().starve(), 1.0f);
        }

        // Random special idle animations from JSON
        if (specialAnimCooldown > 0) {
            specialAnimCooldown--;
        } else if (dino.special_animations != null && !dino.special_animations.isEmpty()
                && !isNavigating() && getTarget() == null && getRandom().nextInt(200) == 0) {
            String anim = dino.special_animations.get(getRandom().nextInt(dino.special_animations.size()));
            triggerAnim(SPECIAL_CONTROLLER, anim);
            specialAnimCooldown = 300;
        }
    }

    // --- Death drops ---

    @Override
    protected void dropLoot(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropLoot(world, source, causedByPlayer);
        Dinosaur dino = getDinosaur();
        if (dino == null) return;

        net.minecraft.item.Item meatItem = Registries.ITEM.get(Identifier.of(FossilsArch2Mod.MOD_ID, dino.id + "_meat"));
        if (meatItem != net.minecraft.item.Items.AIR) {
            int count = 1 + getRandom().nextInt(dino.meat_drop_count);
            dropStack(world, new ItemStack(meatItem, count));
        }
    }

    // --- Sounds ---

    @Override
    protected net.minecraft.sound.SoundEvent getAmbientSound() {
        return mod.fossilsarch2.registry.ModSounds.get(dinosaurId + ".ambient");
    }

    @Override
    protected net.minecraft.sound.SoundEvent getHurtSound(DamageSource source) {
        return mod.fossilsarch2.registry.ModSounds.get(dinosaurId + ".hurt");
    }

    @Override
    protected net.minecraft.sound.SoundEvent getDeathSound() {
        return mod.fossilsarch2.registry.ModSounds.get(dinosaurId + ".death");
    }

    // --- NBT ---

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("FA2DataVersion", CURRENT_DATA_VERSION);
        nbt.putInt("DinoAge", getDinoAge());
        nbt.putInt("Hunger", getHunger());
        nbt.putString("Variant", getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        int savedVersion = nbt.getInt("FA2DataVersion", 0);
        // if (savedVersion < 2) { ... }

        setDinoAge(nbt.getInt("DinoAge", 0));
        setHunger(nbt.getInt("Hunger", getMaxHunger()));
        setVariant(nbt.getString("Variant").orElse(""));
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false; // Dinosaurs don't breed via vanilla mechanics
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) {
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
        if (this.handSwinging) {
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
