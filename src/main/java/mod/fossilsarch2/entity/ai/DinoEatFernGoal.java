package mod.fossilsarch2.entity.ai;

import mod.fossilsarch2.block.FernBlock;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Plant-eating dinosaurs graze mature fern blocks to restore hunger, eating them down one
 * growth stage at a time. Only added for diets that eat plants.
 */
public class DinoEatFernGoal extends DinoFoodSourceGoal {

    private static final int MIN_EDIBLE_AGE = 2;
    private static final int NUTRITION = 15;

    public DinoEatFernGoal(DinosaurEntity dino) {
        super(dino, 60);
    }

    @Override
    protected @Nullable BlockPos findFood() {
        return searchNearby(10, 3, this::isMatureFern);
    }

    @Override
    protected boolean hasFood(BlockPos pos) {
        return dino.level().getBlockState(pos).is(ModBlocks.FERN.get());
    }

    private boolean isMatureFern(BlockPos pos) {
        BlockState state = dino.level().getBlockState(pos);
        return state.is(ModBlocks.FERN.get()) && state.getValue(FernBlock.AGE) >= MIN_EDIBLE_AGE;
    }

    @Override
    protected int eatDuration() {
        return 40;
    }

    @Override
    protected void eat(BlockPos pos) {
        Level level = dino.level();
        BlockState state = level.getBlockState(pos);
        int age = state.getValue(FernBlock.AGE);
        if (age > 0) {
            level.setBlockAndUpdate(pos, state.setValue(FernBlock.AGE, age - 1));
        } else {
            level.destroyBlock(pos, false, dino, 0);
        }
        dino.feed(NUTRITION);
        target = null; // one bite per approach; search again if still hungry
    }
}
