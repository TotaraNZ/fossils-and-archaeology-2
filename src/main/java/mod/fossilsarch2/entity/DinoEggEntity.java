package mod.fossilsarch2.entity;

import java.util.UUID;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.registry.DinosaurRegistry;
import mod.fossilsarch2.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import mod.fossilsarch2.network.DinopediaPayload;
import mod.fossilsarch2.registry.ModAdvancements;
import mod.fossilsarch2.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
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

public class DinoEggEntity extends Entity implements GeoEntity {

    private static final TrackedData<String> DINO_ID = DataTracker.registerData(
            DinoEggEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> HATCH_PROGRESS = DataTracker.registerData(
            DinoEggEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUuid;

    public DinoEggEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient() && player.getStackInHand(hand).isOf(ModItems.DINOPEDIA)) {
            ServerPlayNetworking.send((ServerPlayerEntity) player, new DinopediaPayload(getId()));
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DINO_ID, "");
        builder.add(HATCH_PROGRESS, 0);
    }

    public String getDinoId() {
        return dataTracker.get(DINO_ID);
    }

    public void setDinoId(String id) {
        dataTracker.set(DINO_ID, id);
    }

    public int getHatchProgress() {
        return dataTracker.get(HATCH_PROGRESS);
    }

    public void setHatchProgress(int progress) {
        dataTracker.set(HATCH_PROGRESS, progress);
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Override
    protected double getGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        // Use vanilla physics: applyGravity + move (same pattern as FallingBlockEntity)
        applyGravity();
        move(net.minecraft.entity.MovementType.SELF, getVelocity());
        setVelocity(getVelocity().multiply(0.98));

        super.tick();
        if (getWorld().isClient()) return;

        String dinoId = getDinoId();
        if (dinoId.isEmpty()) {
            discard();
            return;
        }

        Dinosaur dino = DinosaurRegistry.get(Identifier.of(FossilsArch2Mod.MOD_ID, dinoId));
        if (dino == null) {
            discard();
            return;
        }

        int light = getWorld().getLightLevel(getBlockPos());
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
        if (!(getWorld() instanceof ServerWorld serverWorld)) return;

        EntityType<DinosaurEntity> entityType = ModEntities.TYPES.get(dino.id);
        if (entityType == null) {
            discard();
            return;
        }

        DinosaurEntity baby = entityType.create(serverWorld, SpawnReason.BREEDING);
        if (baby == null) {
            discard();
            return;
        }

        baby.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0);
        baby.setDinoAge(0);

        if (ownerUuid != null) {
            PlayerEntity owner = serverWorld.getPlayerByUuid(ownerUuid);
            if (owner != null) {
                baby.setTamedBy(owner);
                if (owner instanceof ServerPlayerEntity serverPlayer) {
                    ModAdvancements.grant(serverPlayer, ModAdvancements.HATCH_DINOSAUR,
                            ModAdvancements.HATCHED_DINOSAUR_CRITERION);
                }
            }
        }

        serverWorld.spawnEntity(baby);
        discard();
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        discard();
        return true;
    }

    private static final int CURRENT_DATA_VERSION = 1;

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        int savedVersion = nbt.getInt("FA2DataVersion", 0);
        // if (savedVersion < 2) { ... }

        setDinoId(nbt.getString("DinoId").orElse(""));
        setHatchProgress(nbt.getInt("HatchProgress", 0));
        nbt.getString("OwnerUUID").ifPresent(s -> {
            try { ownerUuid = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("FA2DataVersion", CURRENT_DATA_VERSION);
        nbt.putString("DinoId", getDinoId());
        nbt.putInt("HatchProgress", getHatchProgress());
        if (ownerUuid != null) {
            nbt.putString("OwnerUUID", ownerUuid.toString());
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
