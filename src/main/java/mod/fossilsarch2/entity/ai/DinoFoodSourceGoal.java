package mod.fossilsarch2.entity.ai;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

/**
 * Base for goals where a hungry dinosaur walks to a food block and eats from it.
 *
 * <p>Reach scales with the dinosaur's hitbox (like {@code MeleeAttackGoal}), so large adults
 * whose centre can never get close to a solid block can still eat. The goal gives up if the
 * path fails or the dinosaur stops getting closer for {@link #PROGRESS_WINDOW} ticks, so a hungry
 * dinosaur never stays stuck walking at food it cannot reach, however far away the food is.
 */
public abstract class DinoFoodSourceGoal extends Goal {

    private static final int PROGRESS_WINDOW = 100;
    private static final double MIN_PROGRESS_SQR = 1.0;
    private static final int REPATH_INTERVAL = 20;
    private static final int UNREACHABLE_MEMORY_TICKS = 1200;

    protected final DinosaurEntity dino;
    private final int searchCooldownTicks;
    protected @Nullable BlockPos target;
    private int searchCooldown;
    private int approachTicks;
    private double distanceAtLastCheck;
    private boolean stuck;
    private int eatTicks;
    /** Food the dinosaur recently failed to reach (e.g. in the next pen), skipped until the tick stored. */
    private final Map<BlockPos, Long> unreachableUntil = new HashMap<>();

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
        return target != null && !stuck && dino.isHungry() && hasFood(target);
    }

    @Override
    public void start() {
        approachTicks = 0;
        eatTicks = 0;
        stuck = false;
        distanceAtLastCheck = distanceToTargetSqr();
        moveToTarget();
    }

    @Override
    public void tick() {
        if (target == null) return;

        if (!isInReach(target)) {
            dino.setEating(false);
            eatTicks = 0;
            if (++approachTicks % PROGRESS_WINDOW == 0) {
                double distance = distanceToTargetSqr();
                stuck = distanceAtLastCheck - distance < MIN_PROGRESS_SQR;
                distanceAtLastCheck = distance;
            }
            if (approachTicks % REPATH_INTERVAL == 0 || dino.getNavigation().isDone()) {
                // Navigation refuses to plan while airborne (just spawned, jumping, falling), so only
                // a failed path from solid ground means the food is genuinely unreachable.
                if (!moveToTarget() && dino.onGround()) {
                    stuck = true;
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
        }
    }

    @Override
    public void stop() {
        if (stuck && target != null) {
            unreachableUntil.put(target, dino.level().getGameTime() + UNREACHABLE_MEMORY_TICKS);
        }
        target = null;
        dino.setEating(false);
        searchCooldown = searchCooldownTicks;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private double distanceToTargetSqr() {
        return target == null ? 0 : dino.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
    }

    /**
     * Path to within reach of the food rather than onto it: a wide dinosaur cannot stand next to a
     * solid block, so a path that must end at the block can stop short or fail entirely.
     */
    private boolean moveToTarget() {
        if (target == null) return false;
        Path path = dino.getNavigation().createPath(target, Mth.floor(reach()));
        return path != null && dino.getNavigation().moveTo(path, 1.0);
    }

    /** How far from its centre a dinosaur can eat: half its width plus a neck's length. */
    private double reach() {
        return dino.getBbWidth() / 2.0 + 2.0;
    }

    /** Horizontal reach from the dinosaur's centre, plus a block-height vertical allowance. */
    protected boolean isInReach(BlockPos pos) {
        double reach = reach();
        double dx = dino.getX() - (pos.getX() + 0.5);
        double dz = dino.getZ() - (pos.getZ() + 0.5);
        double dy = Math.abs(dino.getY() - pos.getY());
        return dx * dx + dz * dz <= reach * reach && dy <= 2.0;
    }

    private boolean isRememberedUnreachable(BlockPos pos) {
        if (unreachableUntil.isEmpty()) return false;
        long now = dino.level().getGameTime();
        unreachableUntil.values().removeIf(until -> until <= now);
        return unreachableUntil.containsKey(pos);
    }

    /** Nearest block (by distance) within a box of the given radii that matches {@code test}. */
    protected @Nullable BlockPos searchNearby(int horizontal, int vertical, Predicate<BlockPos> test) {
        BlockPos origin = dino.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-horizontal, -vertical, -horizontal),
                origin.offset(horizontal, vertical, horizontal))) {
            double dist = pos.distSqr(origin);
            if (dist < bestDist && !isRememberedUnreachable(pos) && test.test(pos)) {
                best = pos.immutable();
                bestDist = dist;
            }
        }
        return best;
    }

    /**
     * Nearest block entity of the given type within a box of the given radii that matches {@code test}.
     * Scans only the block entities of already-loaded chunks, which is far cheaper than probing every
     * position, and never loads chunks.
     */
    protected <T extends BlockEntity> @Nullable BlockPos searchBlockEntities(int horizontal, int vertical,
            Class<T> type,
            Predicate<T> test) {
        BlockPos origin = dino.blockPosition();
        BoundingBox box = BoundingBox.fromCorners(origin.offset(-horizontal, -vertical, -horizontal),
                origin.offset(horizontal, vertical, horizontal));
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = SectionPos.blockToSectionCoord(box.minX()); cx <= SectionPos
                .blockToSectionCoord(box.maxX()); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(box.minZ()); cz <= SectionPos
                    .blockToSectionCoord(box.maxZ()); cz++) {
                LevelChunk chunk = dino.level().getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    double dist = be.getBlockPos().distSqr(origin);
                    if (dist < bestDist && box.isInside(be.getBlockPos()) && !isRememberedUnreachable(be.getBlockPos())
                            && type.isInstance(be)
                            && test.test(type.cast(be))) {
                        best = be.getBlockPos();
                        bestDist = dist;
                    }
                }
            }
        }
        return best;
    }
}
