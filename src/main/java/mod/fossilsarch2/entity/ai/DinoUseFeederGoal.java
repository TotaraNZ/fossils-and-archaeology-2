package mod.fossilsarch2.entity.ai;

import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/** Hungry dinosaurs eat from a nearby feeder holding food their diet allows, until full. */
public class DinoUseFeederGoal extends DinoFoodSourceGoal {

    private static final int NUTRITION_PER_BITE = 20;

    public DinoUseFeederGoal(DinosaurEntity dino) {
        super(dino, 100);
    }

    @Override
    protected @Nullable BlockPos findFood() {
        return searchNearby(16, 4, this::hasFood);
    }

    @Override
    protected boolean hasFood(BlockPos pos) {
        Dinosaur data = dino.getDinosaur();
        return data != null && dino.level().getBlockEntity(pos) instanceof FeederBlockEntity feeder
                && ((data.diet().eatsMeat() && feeder.hasMeat()) || (data.diet().eatsPlants() && feeder.hasVeg()));
    }

    @Override
    protected int eatDuration() {
        return 20;
    }

    @Override
    protected void eat(BlockPos pos) {
        Dinosaur data = dino.getDinosaur();
        if (data == null || !(dino.level().getBlockEntity(pos) instanceof FeederBlockEntity feeder)) return;
        if (data.diet().eatsMeat() && feeder.hasMeat()) {
            feeder.consumeMeat(NUTRITION_PER_BITE);
            dino.feed(NUTRITION_PER_BITE);
        } else if (data.diet().eatsPlants() && feeder.hasVeg()) {
            feeder.consumeVeg(NUTRITION_PER_BITE);
            dino.feed(NUTRITION_PER_BITE);
        }
    }
}
