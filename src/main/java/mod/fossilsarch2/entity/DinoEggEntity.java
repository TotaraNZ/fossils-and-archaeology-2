package mod.fossilsarch2.entity;

import java.util.UUID;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.dinosaur.DinosaurUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import mod.fossilsarch2.network.ModNetworking;
import mod.fossilsarch2.registry.ModAdvancements;
import mod.fossilsarch2.registry.ModItems;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

public class DinoEggEntity extends Entity implements GeoEntity {

    private static final EntityDataAccessor<String> DINO_ID = SynchedEntityData.defineId(
            DinoEggEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> HATCH_PROGRESS = SynchedEntityData.defineId(
            DinoEggEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUuid;

    public DinoEggEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 hitPos) {
        if (!level().isClientSide() && player.getItemInHand(hand).is(ModItems.DINOPEDIA)) {
            ModNetworking.sendDinopedia((ServerPlayer) player, getId());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DINO_ID, "");
        builder.define(HATCH_PROGRESS, 0);
    }

    /** The species this egg hatches into, e.g. {@code fossilsarch2:triceratops}. */
    public @Nullable Identifier getSpeciesId() {
        return DinosaurUtils.parseSpeciesId(entityData.get(DINO_ID));
    }

    public void setSpeciesId(Identifier speciesId) {
        entityData.set(DINO_ID, speciesId.toString());
    }

    public @Nullable Dinosaur getDinosaur() {
        return DinosaurUtils.get(level(), getSpeciesId());
    }

    public int getHatchProgress() {
        return entityData.get(HATCH_PROGRESS);
    }

    public void setHatchProgress(int progress) {
        entityData.set(HATCH_PROGRESS, progress);
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        // Use vanilla physics: applyGravity + move (same pattern as FallingBlockEntity)
        applyGravity();
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.98));

        super.tick();
        if (level().isClientSide()) return;

        Dinosaur dino = getDinosaur();
        if (dino == null) {
            discard();
            return;
        }

        int light = level().getMaxLocalRawBrightness(blockPosition());
        int progress = getHatchProgress();

        if (light >= 9) {
            progress++;
        } else {
            progress--;
        }

        if (progress >= dino.growth().hatchTime()) {
            hatch(dino);
            return;
        }

        if (progress <= -dino.growth().hatchTime()) {
            discard();
            return;
        }

        setHatchProgress(progress);
    }

    private void hatch(Dinosaur dino) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        // Species and entity type share an ID, so addon species hatch into their own entity type.
        Identifier speciesId = getSpeciesId();
        EntityType<?> entityType = speciesId != null ? BuiltInRegistries.ENTITY_TYPE.getValue(speciesId) : null;
        if (entityType == null || !(entityType.create(serverLevel, EntitySpawnReason.BREEDING) instanceof DinosaurEntity baby)) {
            discard();
            return;
        }

        baby.snapTo(getX(), getY(), getZ(), getYRot(), 0);
        baby.setDinoAge(0);

        ServerPlayer serverPlayerOwner = null;
        if (ownerUuid != null) {
            Player owner = serverLevel.getPlayerInAnyDimension(ownerUuid);
            if (owner != null) {
                baby.setTamedBy(owner);
                if (owner instanceof ServerPlayer serverPlayer) {
                    serverPlayerOwner = serverPlayer;
                }
            }
        }

        if (serverLevel.addFreshEntity(baby) && serverPlayerOwner != null) {
            ModAdvancements.grant(serverPlayerOwner, ModAdvancements.HATCH_DINOSAUR,
                    ModAdvancements.HATCHED_DINOSAUR_CRITERION);
        }
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        discard();
        return true;
    }

    private static final int CURRENT_DATA_VERSION = 1;

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        int savedVersion = input.getIntOr("FA2DataVersion", 0);
        // if (savedVersion < 2) { ... }

        // Pre-namespace saves stored a bare path ("triceratops"); parseSpeciesId defaults the namespace.
        entityData.set(DINO_ID, input.getStringOr("DinoId", ""));
        setHatchProgress(input.getIntOr("HatchProgress", 0));
        input.getString("OwnerUUID").ifPresent(s -> {
            try { ownerUuid = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("FA2DataVersion", CURRENT_DATA_VERSION);
        output.putString("DinoId", entityData.get(DINO_ID));
        output.putInt("HatchProgress", getHatchProgress());
        if (ownerUuid != null) {
            output.putString("OwnerUUID", ownerUuid.toString());
        }
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("idle", 0, this::idleController));
    }

    private PlayState idleController(AnimationTest<DinoEggEntity> test) {
        return test.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
