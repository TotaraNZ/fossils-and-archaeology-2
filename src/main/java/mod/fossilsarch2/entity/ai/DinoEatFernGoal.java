package mod.fossilsarch2.entity.ai;

import mod.fossilsarch2.block.FernBlock;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Herbivore/omnivore dinosaurs eat mature fern blocks to restore hunger.
 * Follows the same pattern as vanilla EatGrassGoal.
 */
public class DinoEatFernGoal extends Goal {

    private final DinosaurEntity dino;
    private BlockPos fernPos;
    private int eatingTimer;

    public DinoEatFernGoal(DinosaurEntity dino) {
        this.dino = dino;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!dino.isHungry()) return false;
        fernPos = findNearbyFern();
        return fernPos != null;
    }

    @Override
    public boolean shouldContinue() {
        return fernPos != null && eatingTimer > 0;
    }

    @Override
    public void start() {
        eatingTimer = 40; // 2 seconds to eat
        dino.getNavigation().startMovingTo(
                fernPos.getX() + 0.5, fernPos.getY(), fernPos.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (fernPos == null) return;

        double dist = dino.squaredDistanceTo(
                fernPos.getX() + 0.5, fernPos.getY() + 0.5, fernPos.getZ() + 0.5);

        if (dist > 4.0) {
            dino.getNavigation().startMovingTo(
                    fernPos.getX() + 0.5, fernPos.getY(), fernPos.getZ() + 0.5, 1.0);
            return;
        }

        // Close enough — look at the fern and eat
        dino.getLookControl().lookAt(fernPos.getX() + 0.5, fernPos.getY() + 0.5, fernPos.getZ() + 0.5);
        dino.getNavigation().stop();
        dino.setEating(true);
        eatingTimer--;

        if (eatingTimer <= 0) {
            World world = dino.getWorld();
            BlockState state = world.getBlockState(fernPos);
            if (state.isOf(ModBlocks.FERN)) {
                int age = state.get(FernBlock.AGE);
                if (age > 0) {
                    // Reduce fern age by 1 (eaten down)
                    world.setBlockState(fernPos, state.with(FernBlock.AGE, age - 1));
                } else {
                    // Destroy seedling
                    world.breakBlock(fernPos, false);
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
    }

    private BlockPos findNearbyFern() {
        BlockPos dinoPos = dino.getBlockPos();
        World world = dino.getWorld();

        for (BlockPos pos : BlockPos.iterateOutwards(dinoPos, 10, 3, 10)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(ModBlocks.FERN) && state.get(FernBlock.AGE) >= 2) {
                return pos.toImmutable();
            }
        }
        return null;
    }
}
