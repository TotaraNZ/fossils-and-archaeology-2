package mod.fossilsarch2.entity;

import java.util.UUID;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.registry.DinosaurRegistry;
import mod.fossilsarch2.registry.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import mod.fossilsarch2.network.DinopediaPayload;
import mod.fossilsarch2.registry.ModAdvancements;
import mod.fossilsarch2.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
        if (!level().isClientSide() && player.getItemInHand(hand).getItem() == ModItems.DINOPEDIA) {
            ServerPlayNetworking.send((ServerPlayer) player, new DinopediaPayload(getId()));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DINO_ID, "");
        builder.define(HATCH_PROGRESS, 0);
    }

    public String getDinoId() {
        return entityData.get(DINO_ID);
    }

    public void setDinoId(String id) {
        entityData.set(DINO_ID, id);
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

        String dinoId = getDinoId();
        if (dinoId.isEmpty()) {
            discard();
            return;
        }

        Dinosaur dino = DinosaurRegistry.get(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, dinoId));
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

        if (progress >= dino.hatch_time) {
            hatch(dino);
            return;
        }

        if (progress <= -dino.hatch_time) {
            discard();
            return;
        }

        setHatchProgress(progress);
    }

    private void hatch(Dinosaur dino) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        EntityType<DinosaurEntity> entityType = ModEntities.TYPES.get(dino.id);
        if (entityType == null) {
            discard();
            return;
        }

        DinosaurEntity baby = entityType.create(serverLevel, EntitySpawnReason.BREEDING);
        if (baby == null) {
            discard();
            return;
        }

        baby.snapTo(getX(), getY(), getZ(), getYRot(), 0);
        baby.setDinoAge(0);

        if (ownerUuid != null) {
            Player owner = serverLevel.getPlayerInAnyDimension(ownerUuid);
            if (owner != null) {
                baby.setTamedBy(owner);
                if (owner instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.grant(serverPlayer, ModAdvancements.HATCH_DINOSAUR,
                            ModAdvancements.HATCHED_DINOSAUR_CRITERION);
                }
            }
        }

        serverLevel.addFreshEntity(baby);
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

        setDinoId(input.getStringOr("DinoId", ""));
        setHatchProgress(input.getIntOr("HatchProgress", 0));
        input.getString("OwnerUUID").ifPresent(s -> {
            try { ownerUuid = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("FA2DataVersion", CURRENT_DATA_VERSION);
        output.putString("DinoId", getDinoId());
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
