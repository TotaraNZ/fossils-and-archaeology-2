package mod.fossilsarch2.entity.ai;

import mod.fossilsarch2.block.FernBlock;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.registry.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Herbivore/omnivore dinosaurs eat mature fern blocks to restore hunger.
 * Follows the same pattern as vanilla EatGrassGoal.
 */
public class DinoEatFernGoal extends Goal {

    private final DinosaurEntity dino;
    private BlockPos fernPos;
    private int eatingTimer;
    private int searchCooldown;

    public DinoEatFernGoal(DinosaurEntity dino) {
        this.dino = dino;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (searchCooldown > 0) { searchCooldown--; return false; }
        if (!dino.isHungry()) return false;
        fernPos = findNearbyFern();
        if (fernPos == null) {
            searchCooldown = 40;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return fernPos != null && eatingTimer > 0;
    }

    @Override
    public void start() {
        eatingTimer = 40; // 2 seconds to eat
        dino.getNavigation().moveTo(
                fernPos.getX() + 0.5, fernPos.getY(), fernPos.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (fernPos == null) return;

        double dist = dino.distanceToSqr(
                fernPos.getX() + 0.5, fernPos.getY() + 0.5, fernPos.getZ() + 0.5);

        if (dist > 4.0) {
            dino.getNavigation().moveTo(
                    fernPos.getX() + 0.5, fernPos.getY(), fernPos.getZ() + 0.5, 1.0);
            return;
        }

        // Close enough — look at the fern and eat
        dino.getLookControl().setLookAt(fernPos.getX() + 0.5, fernPos.getY() + 0.5, fernPos.getZ() + 0.5);
        dino.getNavigation().stop();
        dino.setEating(true);
        eatingTimer--;

        if (eatingTimer <= 0) {
            Level level = dino.level();
            BlockState state = level.getBlockState(fernPos);
            if (state.getBlock() == ModBlocks.FERN) {
                int age = state.getValue(FernBlock.AGE);
                if (age > 0) {
                    // Reduce fern age by 1 (eaten down)
                    level.setBlockAndUpdate(fernPos, state.setValue(FernBlock.AGE, age - 1));
                } else {
                    // Destroy seedling
                    level.destroyBlock(fernPos, false, dino, 0);
                }
                dino.feed(15);
            }
            fernPos = null;
        }
    }

    @Override
    public void stop() {
        fernPos = null;
        eatingTimer = 0;
        dino.setEating(false);
        searchCooldown = 60;
    }

    private BlockPos findNearbyFern() {
        BlockPos dinoPos = dino.blockPosition();
        Level level = dino.level();

        for (BlockPos pos : BlockPos.withinManhattan(dinoPos, 10, 3, 10)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() == ModBlocks.FERN && state.getValue(FernBlock.AGE) >= 2) {
                return pos.immutable();
            }
        }
        return null;
    }
}
