package mod.fossilsarch2.entity.ai;

import java.util.EnumSet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Follows the owner while they are on or in the water. Vanilla's FollowOwnerGoal only supports
 * walking and flying mobs and teleports onto land, which would strand an aquatic animal.
 */
public class DinoSwimAfterOwnerGoal extends Goal {
    private final TamableAnimal mob;
    private final double speedModifier;
    private final float startDistance;
    private final float stopDistance;
    private @Nullable LivingEntity owner;
    private int recalcPathIn;

    public DinoSwimAfterOwnerGoal(TamableAnimal mob, double speedModifier, float startDistance, float stopDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity candidate = mob.getOwner();
        if (candidate == null || mob.unableToMoveToOwner() || !isOnWater(candidate)
                || mob.distanceToSqr(candidate) < startDistance * startDistance) {
            return false;
        }
        owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && !mob.getNavigation().isDone() && !mob.unableToMoveToOwner() && isOnWater(owner)
                && mob.distanceToSqr(owner) > stopDistance * stopDistance;
    }

    @Override
    public void start() {
        recalcPathIn = 0;
    }

    @Override
    public void stop() {
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        mob.getLookControl().setLookAt(owner, 10.0f, mob.getMaxHeadXRot());
        if (--recalcPathIn <= 0) {
            recalcPathIn = adjustedTickDelay(10);
            mob.getNavigation().moveTo(owner, speedModifier);
        }
    }

    /** Swimming, or in a boat (or other vehicle) that is floating. */
    private static boolean isOnWater(LivingEntity owner) {
        Entity vehicle = owner.getVehicle();
        return owner.isInWater() || vehicle != null && vehicle.isInWater();
    }
}
