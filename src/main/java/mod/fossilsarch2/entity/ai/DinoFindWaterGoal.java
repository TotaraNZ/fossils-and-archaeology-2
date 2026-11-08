package mod.fossilsarch2.entity.ai;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Sends a stranded aquatic animal back to the nearest water before it dries out. */
public class DinoFindWaterGoal extends Goal {
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_HEIGHT = 4;

    private final PathfinderMob mob;
    private final double speedModifier;
    private BlockPos water;

    public DinoFindWaterGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.isInWater() || mob.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        // Nearest first: positions come out in order of increasing Manhattan distance.
        for (BlockPos pos : BlockPos.withinClippedManhattan(mob.blockPosition(), SEARCH_RADIUS, SEARCH_HEIGHT,
                SEARCH_RADIUS)) {
            if (mob.level().getFluidState(pos).is(FluidTags.WATER)) {
                water = pos.immutable();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.isInWater() && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, speedModifier);
    }
}
