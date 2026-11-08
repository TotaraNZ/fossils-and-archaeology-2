package mod.fossilsarch2.entity.ai;

import java.util.EnumSet;

import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Base for goals where a hungry dinosaur walks to a food block and eats from it.
 *
 * <p>Reach scales with the dinosaur's hitbox (like {@code MeleeAttackGoal}), so large adults
 * whose centre can never get close to a solid block can still eat. The goal gives up if the
 * path fails or the food is not reached within {@link #MAX_APPROACH_TICKS}, so a hungry
 * dinosaur never stays stuck walking at food it cannot get to.
 */
public abstract class DinoFoodSourceGoal extends Goal {

    private static final int MAX_APPROACH_TICKS = 200;
    private static final int REPATH_INTERVAL = 20;

    protected final DinosaurEntity dino;
    private final int searchCooldownTicks;
    protected @Nullable BlockPos target;
    private int searchCooldown;
    private int approachTicks;
    private int eatTicks;

    protected DinoFoodSourceGoal(DinosaurEntity dino, int searchCooldownTicks) {
        this.dino = dino;
        this.searchCooldownTicks = searchCooldownTicks;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Nearest suitable food block, or null. */
    protected abstract @Nullable BlockPos findFood();

    /** Whether the block at {@code pos} still offers food this dinosaur can eat. */
    protected abstract boolean hasFood(BlockPos pos);

    /** Eat from {@code pos}. Called once the dinosaur has been in reach for {@link #eatDuration()} ticks. */
    protected abstract void eat(BlockPos pos);

    /** Ticks spent eating (and showing the eat animation) before {@link #eat} applies. */
    protected abstract int eatDuration();

    @Override
    public boolean canUse() {
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }
        if (!dino.isHungry()) return false;
        target = findFood();
        if (target == null) {
            searchCooldown = searchCooldownTicks;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && dino.isHungry() && approachTicks < MAX_APPROACH_TICKS && hasFood(target);
    }

    @Override
    public void start() {
        approachTicks = 0;
        eatTicks = 0;
        moveToTarget();
    }

    @Override
    public void tick() {
        if (target == null) return;

        if (!isInReach(target)) {
            dino.setEating(false);
            eatTicks = 0;
            approachTicks++;
            if (approachTicks % REPATH_INTERVAL == 0 || dino.getNavigation().isDone()) {
                if (!moveToTarget()) {
                    target = null; // unreachable: give up and let the search cooldown apply
                }
            }
            return;
        }

        dino.getNavigation().stop();
        dino.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        dino.setEating(true);
        if (++eatTicks >= eatDuration()) {
            eat(target);
            eatTicks = 0;
            approachTicks = 0;
        }
    }

    @Override
    public void stop() {
        target = null;
        dino.setEating(false);
        searchCooldown = searchCooldownTicks;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean moveToTarget() {
        return target != null
                && dino.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
    }

    /** Horizontal reach from the dinosaur's edge, plus a block-height vertical allowance. */
    protected boolean isInReach(BlockPos pos) {
        double reach = dino.getBbWidth() / 2.0 + 1.5;
        double dx = dino.getX() - (pos.getX() + 0.5);
        double dz = dino.getZ() - (pos.getZ() + 0.5);
        double dy = Math.abs(dino.getY() - pos.getY());
        return dx * dx + dz * dz <= reach * reach && dy <= 2.0;
    }

    /** Nearest position (Manhattan order) within the given radii that matches {@code test}. */
    protected @Nullable BlockPos searchNearby(int horizontal, int vertical,
            java.util.function.Predicate<BlockPos> test) {
        BlockPos origin = dino.blockPosition();
        for (BlockPos pos : BlockPos.withinManhattan(origin, horizontal + vertical)) {
            if (Math.abs(pos.getY() - origin.getY()) > vertical
                    || Math.abs(pos.getX() - origin.getX()) > horizontal
                    || Math.abs(pos.getZ() - origin.getZ()) > horizontal) {
                continue;
            }
            if (test.test(pos)) return pos.immutable();
        }
        return null;
    }
}
