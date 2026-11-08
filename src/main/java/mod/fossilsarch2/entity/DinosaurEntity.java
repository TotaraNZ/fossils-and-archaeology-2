package mod.fossilsarch2.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.dinosaur.DinosaurUtils;
import mod.fossilsarch2.entity.ai.DinoDefensiveMeleeGoal;
import mod.fossilsarch2.entity.ai.DinoEatFernGoal;
import mod.fossilsarch2.entity.ai.DinoUseFeederGoal;
import mod.fossilsarch2.registry.ModItemTags;
import mod.fossilsarch2.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class DinosaurEntity extends TamableAnimal implements GeoEntity {

    public enum GrowthStage {
        HATCHLING,
        JUVENILE,
        SUBADULT,
        ADULT
    }

    private static final String ATTACK_CONTROLLER = "Attack";
    private static final String ATTACK_TRIGGER = "attack";
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

    private static final String GROWTH_TICKS_KEY = "GrowthTicks";

    /** Species ID; always equal to this entity's type ID (e.g. {@code fossilsarch2:triceratops}). */
    private final Identifier speciesId;
    private @Nullable Dinosaur cachedDinosaur;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** Ticks accumulated toward the next age stage. Saved, so growth survives chunk unloads. */
    private int growthTicks;
    /** Growth-derived attributes are applied once on the first server tick (covers /summon and spawn eggs). */
    private boolean growthStatsApplied;

    public DinosaurEntity(EntityType<? extends DinosaurEntity> type, Level level) {
        super(type, level);
        this.speciesId = EntityType.getKey(type);

        // registerGoals() is called from super() before speciesId is set,
        // so diet-dependent goals must be added here after construction
        setupDietGoals();

        // Assign random variant if species has them and none set
        Dinosaur dino = getDinosaur();
        if (dino != null && !dino.variants().isEmpty() && getVariant().isEmpty()) {
            setVariant(dino.variants().get(getRandom().nextInt(dino.variants().size())));
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
        // Only register diet-independent goals here (speciesId is null at this point)
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new DinoUseFeederGoal(this));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1D, stack -> stack.is(ModItems.FERN_SEED), false));
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
            FossilsArch2Mod.LOGGER.warn("setupDietGoals: no dinosaur data for species '{}'", speciesId);
            return;
        }
        Dinosaur.Diet diet = dino.diet();
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 10.0f, 1.0D,
                Math.max(1.15f, dino.combat().fleeSpeed()), this::shouldFear));

        if (diet.eatsPlants()) {
            this.goalSelector.addGoal(4, new DinoEatFernGoal(this));
        }

        if (diet.eatsMeat()) {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, dino.combat().chaseSpeed(), false));
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, 10, false, false,
                    (target, level) -> this.shouldHunt(target)));
            this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                    (target, level) -> target instanceof Player player && this.shouldAttackPlayer(player)));
        }

        if (diet == Dinosaur.Diet.HERBIVORE) {
            this.goalSelector.addGoal(2, new DinoDefensiveMeleeGoal(this, dino.combat().chaseSpeed()));
            this.goalSelector.addGoal(3, new PanicGoal(this, dino.combat().fleeSpeed()));
        }
    }

    public static AttributeSupplier.Builder createAttributes(Dinosaur d) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, d.combat().health())
                .add(Attributes.MOVEMENT_SPEED, d.combat().speed())
                .add(Attributes.ATTACK_DAMAGE, d.combat().attackDamage())
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
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
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.TEMPT_RANGE, 16.0);
    }

    // --- Dinosaur Data ---

    public Identifier getSpeciesId() {
        return speciesId;
    }

    /**
     * The species data for this entity. Dinosaur entries live in a world-layer datapack registry,
     * which is fixed for the lifetime of a level, so the lookup is cached.
     */
    public @Nullable Dinosaur getDinosaur() {
        if (cachedDinosaur == null && speciesId != null) {
            cachedDinosaur = DinosaurUtils.get(level(), speciesId);
        }
        return cachedDinosaur;
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
        int clampedAge = dino != null ? Math.max(0, Math.min(age, dino.growth().maxAge())) : Math.max(0, age);
        if (entityData.get(DINO_AGE) != clampedAge) {
            // onSyncedDataUpdated applies stats and refreshes dimensions
            entityData.set(DINO_AGE, clampedAge);
        } else {
            applyGrowthDerivedStats();
        }
        growthTicks = 0;
    }

    public boolean growByStages(int stages) {
        Dinosaur dino = getDinosaur();
        if (dino == null || stages <= 0 || getDinoAge() >= dino.growth().maxAge()) {
            return false;
        }

        setDinoAge(Math.min(getDinoAge() + stages, dino.growth().maxAge()));
        return true;
    }

    /** True until the dinosaur reaches its species' max age. Vanilla {@link #isBaby()} is final and age-driven. */
    public boolean isJuvenile() {
        Dinosaur dino = getDinosaur();
        return dino != null && getDinoAge() < dino.growth().maxAge();
    }

    public GrowthStage getGrowthStage() {
        return DinosaurCombat.getGrowthStage(this);
    }

    @Override
    public float getAgeScale() {
        return getScaleFactor();
    }

    public float getScaleFactor() {
        Dinosaur dino = getDinosaur();
        if (dino == null) return 1.0f;
        if (getDinoAge() >= dino.growth().maxAge()) return dino.growth().adultScale();
        float progress = (float) getDinoAge() / dino.growth().maxAge();
        return dino.growth().babyScale() + (dino.growth().adultScale() - dino.growth().babyScale()) * progress;
    }

    public double getBodyMass() {
        return DinosaurCombat.getBodyMass(this);
    }

    public double getAdultBodyMass() {
        return DinosaurCombat.getAdultBodyMass(this);
    }

    public double getCombatPower() {
        return DinosaurCombat.getCombatPower(this);
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
        int threshold = dino != null ? dino.hunger().hungryThreshold() : 80;
        return getHunger() < threshold;
    }

    public int getMaxHunger() {
        Dinosaur dino = getDinosaur();
        return dino != null ? dino.hunger().maxHunger() : DEFAULT_MAX_HUNGER;
    }

    public float getGrowthProgress() {
        Dinosaur dino = getDinosaur();
        if (dino == null || dino.growth().maxAge() <= 0) return 1.0f;
        return (float) getDinoAge() / dino.growth().maxAge();
    }

    public boolean isDefensivelyMature() {
        return DinosaurCombat.isDefensivelyMature(this);
    }

    public boolean shouldHunt(LivingEntity target) {
        return DinosaurCombat.shouldHunt(this, target);
    }

    public boolean shouldAttackPlayer(Player target) {
        return DinosaurCombat.shouldAttackPlayer(this, target);
    }

    public boolean shouldFear(LivingEntity target) {
        return DinosaurCombat.shouldFear(this, target);
    }

    /** Server-only: scale attributes to the current growth stage, preserving the health fraction. */
    private void applyGrowthDerivedStats() {
        if (level().isClientSide()) return;
        Dinosaur dino = getDinosaur();
        if (dino == null) return;
        growthStatsApplied = true;

        double currentMaxHealth = Math.max(1.0, getMaxHealth());
        double healthFraction = getHealth() / currentMaxHealth;
        setBaseAttribute(Attributes.MAX_HEALTH, dino.combat().health() * DinosaurCombat.getHealthScaleFactor(this));
        setBaseAttribute(Attributes.ATTACK_DAMAGE,
                dino.combat().attackDamage() * DinosaurCombat.getAttackScaleFactor(this));
        setBaseAttribute(Attributes.KNOCKBACK_RESISTANCE, DinosaurCombat.getKnockbackResistanceForGrowth(this));

        float scaledMaxHealth = (float) Math.max(1.0, getAttributeValue(Attributes.MAX_HEALTH));
        float scaledHealth = (float) Math.max(0.1, Math.min(scaledMaxHealth, scaledMaxHealth * healthFraction));
        setHealth(scaledHealth);
    }

    private void setBaseAttribute(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
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
            // Attributes and health are server-authoritative and synced separately;
            // the client only needs its bounding box resized.
            applyGrowthDerivedStats();
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
        if (stack.is(ModItems.DINOPEDIA) && !level().isClientSide()) {
            mod.fossilsarch2.network.ModNetworking.sendDinopedia(
                    (net.minecraft.server.level.ServerPlayer) player, getId());
            return InteractionResult.SUCCESS;
        }

        if (stack.is(ModItems.ESSENCE_CHICKEN)) {
            if (!level().isClientSide()) {
                if (!isJuvenile()) {
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
                float tameChance = dinoData != null ? dinoData.hunger().tameChance() : 0.33f;
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

        return (dino.diet().eatsPlants() && stack.is(ModItems.FERN_SEED))
                || (dino.diet().eatsMeat() && stack.is(ModItemTags.DINO_MEAT));
    }

    private static void consumeHeldItemAndGiveRemainder(Player player, InteractionHand hand, ItemStack heldStack,
            ItemStack remainder) {
        heldStack.shrink(1);
        if (heldStack.isEmpty()) {
            player.setItemInHand(hand, remainder);
        } else if (!player.getInventory().add(remainder)) {
            player.drop(remainder, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        Dinosaur dino = getDinosaur();
        if (dino == null) return;

        if (!growthStatsApplied) {
            applyGrowthDerivedStats();
        }

        // Growth: a saved counter rather than tickCount, which resets whenever the entity reloads
        if (getDinoAge() < dino.growth().maxAge() && ++growthTicks >= dino.growth().growTime()) {
            setDinoAge(getDinoAge() + 1);
        }

        // Hunger decrease
        int decayRate = dino.hunger().hungerDecayRate();
        if (decayRate > 0 && tickCount % decayRate == 0) {
            setHunger(getHunger() - 1);
        }

        // Starvation
        if (getHunger() <= 0 && tickCount % 40 == 0) {
            hurtServer((ServerLevel) level(), damageSources().starve(), 1.0f);
        }

        LivingEntity currentTarget = getTarget();
        if (currentTarget != null) {
            boolean keepTarget = currentTarget.isAlive();
            if (keepTarget && currentTarget instanceof Player player) {
                keepTarget = shouldAttackPlayer(player);
            } else if (keepTarget && currentTarget instanceof Animal animal) {
                keepTarget = shouldHunt(animal) || getLastHurtByMob() == currentTarget;
            }

            if (!keepTarget || shouldFear(currentTarget)) {
                setTarget(null);
            }
        }

        // Random special idle animations from JSON
        if (specialAnimCooldown > 0) {
            specialAnimCooldown--;
        } else if (dino.animations().special() != null && !dino.animations().special().isEmpty()
                && getNavigation().isDone() && getTarget() == null && getRandom().nextInt(200) == 0) {
            String anim = dino.animations().special().get(getRandom().nextInt(dino.animations().special().size()));
            // Triggered server-side; GeckoLib syncs the trigger to tracking clients
            triggerAnim(SPECIAL_CONTROLLER, anim);
            specialAnimCooldown = 300;
        }
    }

    // --- Combat ---

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            triggerAnim(ATTACK_CONTROLLER, ATTACK_TRIGGER);
        }
        return hit;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim, DamageSource source) {
        // Hunting is part of the hunger loop: a kill restores hunger in proportion to the prey's size.
        Dinosaur dino = getDinosaur();
        if (dino != null && dino.diet().eatsMeat()) {
            feed(Math.max(5, Math.min(40, Math.round(victim.getMaxHealth()))));
        }
        return super.killedEntity(level, victim, source);
    }

    // --- Death drops ---

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        super.dropCustomDeathLoot(level, source, causedByPlayer);
        Dinosaur dino = getDinosaur();
        if (dino == null) return;

        net.minecraft.world.item.Item meatItem = BuiltInRegistries.ITEM.getValue(speciesId.withSuffix("_meat"));
        if (meatItem != net.minecraft.world.item.Items.AIR && dino.food().meatDropCount() > 0) {
            int count = 1 + getRandom().nextInt(dino.food().meatDropCount());
            spawnAtLocation(level, new ItemStack(meatItem, count));
        }
    }

    // --- Sounds ---

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return speciesSound(".ambient");
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return speciesSound(".hurt");
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return speciesSound(".death");
    }

    /** Sounds follow the convention {@code <ns>:<species>.<kind>}; missing ones are silent. */
    private net.minecraft.sounds.@Nullable SoundEvent speciesSound(String suffix) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(speciesId.withSuffix(suffix)).orElse(null);
    }

    // --- NBT ---

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("FA2DataVersion", CURRENT_DATA_VERSION);
        output.putInt("DinoAge", getDinoAge());
        output.putInt("Hunger", getHunger());
        output.putString("Variant", getVariant());
        output.putInt(GROWTH_TICKS_KEY, growthTicks);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Migrations go here, keyed on input.getIntOr("FA2DataVersion", 0) < CURRENT_DATA_VERSION.

        setDinoAge(input.getIntOr("DinoAge", 0));
        growthTicks = input.getIntOr(GROWTH_TICKS_KEY, 0);
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
        // Animation names come from the species JSON, so a new species needs no animation code.
        Dinosaur.Animations anims = getDinosaur() != null ? getDinosaur().animations() : Dinosaur.Animations.DEFAULT;
        RawAnimation walk = loop(anims.walk());
        RawAnimation idle = loop(anims.idle());
        RawAnimation eat = loop(anims.eat());

        controllers.add(new AnimationController<DinosaurEntity>("Movement", 5,
                test -> play(test, test.isMoving() ? walk : idle)));

        // Attacks are one-shot triggers fired from doHurtTarget so they always play to completion;
        // between attacks the controller shows the eating loop while the dinosaur feeds.
        AnimationController<DinosaurEntity> attack = new AnimationController<>(ATTACK_CONTROLLER, 5,
                test -> isEating() ? play(test, eat) : PlayState.STOP);
        if (!anims.attack().isEmpty()) {
            attack.triggerableAnim(ATTACK_TRIGGER, RawAnimation.begin().thenPlay(anims.attack()));
        }
        controllers.add(attack);

        if (!anims.special().isEmpty()) {
            AnimationController<DinosaurEntity> special = new AnimationController<>(SPECIAL_CONTROLLER, 5,
                    test -> PlayState.STOP);
            for (String animName : anims.special()) {
                special.triggerableAnim(animName, RawAnimation.begin().thenPlay(animName));
            }
            controllers.add(special);
        }
    }

    private static @Nullable RawAnimation loop(String name) {
        return name.isEmpty() ? null : RawAnimation.begin().thenLoop(name);
    }

    private static PlayState play(AnimationTest<DinosaurEntity> test, @Nullable RawAnimation animation) {
        return animation != null ? test.setAndContinue(animation) : PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
