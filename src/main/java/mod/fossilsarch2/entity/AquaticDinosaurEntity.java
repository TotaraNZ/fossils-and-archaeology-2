package mod.fossilsarch2.entity;

import mod.fossilsarch2.entity.ai.DinoFindWaterGoal;
import mod.fossilsarch2.entity.ai.DinoSwimAfterOwnerGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;

import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * A dinosaur with {@code "habitat": "aquatic"}: swims in three dimensions, hunts sea life, and
 * dries out after five minutes on land (like an axolotl), heading back to water when stranded.
 * Everything else (growth, hunger, taming, combat, animations) is shared with land species.
 */
public class AquaticDinosaurEntity extends DinosaurEntity {

    /** Ticks an aquatic dinosaur survives out of water before drying out. */
    public static final int MAX_AIR_OUT_OF_WATER = 6000;

    // Swimming acceleration is speed^2 * this, so a 0.3 movement speed cruises at about 5 blocks/s.
    private static final float IN_WATER_SPEED = 0.3f;
    // Stranded on land, aquatic species can only flop slowly toward water.
    private static final float ON_LAND_SPEED = 0.25f;

    public AquaticDinosaurEntity(EntityType<? extends DinosaurEntity> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0f);
        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, IN_WATER_SPEED, ON_LAND_SPEED, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    @Override
    protected void registerMovementGoals() {
        this.goalSelector.addGoal(0, new DinoFindWaterGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new DinoSwimAfterOwnerGoal(this, 1.0D, 10.0F, 3.0F));
        this.goalSelector.addGoal(7, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RandomSwimmingGoal(this, 1.0D, 40));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isAquatic() {
        return true;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        moveRelative(getSpeed(), input);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.9));
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    // --- Air: unlimited underwater, a countdown on land ---

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public int getMaxAirSupply() {
        return MAX_AIR_OUT_OF_WATER;
    }

    @Override
    public void baseTick() {
        int airSupply = getAirSupply();
        super.baseTick();
        if (!isNoAi() && level() instanceof ServerLevel serverLevel) {
            handleAirSupply(serverLevel, airSupply);
        }
    }

    private void handleAirSupply(ServerLevel level, int preTickAirSupply) {
        if (isAlive() && !isInWaterOrRain()) {
            setAirSupply(preTickAirSupply - 1);
            if (shouldTakeDrowningDamage()) {
                setAirSupply(0);
                hurtServer(level, damageSources().dryOut(), 2.0f);
            }
        } else {
            setAirSupply(getMaxAirSupply());
        }
    }

    /** Teleporting to an owner always lands on dry ground, so aquatic dinosaurs swim after them instead. */
    @Override
    public boolean shouldTryTeleportToOwner() {
        return false;
    }
}
