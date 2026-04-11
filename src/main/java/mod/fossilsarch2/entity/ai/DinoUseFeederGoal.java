package mod.fossilsarch2.entity.ai;

import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class DinoUseFeederGoal extends Goal {

    private final DinosaurEntity dino;
    private BlockPos feederPos;
    private int cooldown = 0;

    public DinoUseFeederGoal(DinosaurEntity dino) {
        this.dino = dino;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (cooldown > 0) { cooldown--; return false; }
        if (!dino.isHungry()) return false;

        feederPos = findNearbyFeeder();
        return feederPos != null;
    }

    @Override
    public boolean shouldContinue() {
        return dino.isHungry() && feederPos != null;
    }

    @Override
    public void start() {
        dino.getNavigation().startMovingTo(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (feederPos == null) return;

        double dist = dino.squaredDistanceTo(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5);
        if (dist < 4.0) {
            // Close enough to eat
            World world = dino.getWorld();
            if (world.getBlockEntity(feederPos) instanceof FeederBlockEntity feeder) {
                Dinosaur dinoData = dino.getDinosaur();
                boolean fed = false;

                if (dinoData != null && dinoData.diet != Dinosaur.Diet.HERBIVORE && feeder.hasMeat()) {
                    feeder.consumeMeat(20);
                    dino.feed(20);
                    fed = true;
                }
                if (dinoData != null && dinoData.diet != Dinosaur.Diet.CARNIVORE && feeder.hasVeg()) {
                    feeder.consumeVeg(20);
                    dino.feed(20);
                    fed = true;
                }

                if (!fed) feederPos = null;
            } else {
                feederPos = null;
            }
            cooldown = 100; // Don't spam feeder checks
        } else {
            dino.getNavigation().startMovingTo(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        feederPos = null;
        cooldown = 200;
    }

    private BlockPos findNearbyFeeder() {
        BlockPos dinoPos = dino.getBlockPos();
        World world = dino.getWorld();

        for (BlockPos pos : BlockPos.iterateOutwards(dinoPos, 16, 4, 16)) {
            if (world.getBlockEntity(pos) instanceof FeederBlockEntity feeder) {
                Dinosaur dinoData = dino.getDinosaur();
                if (dinoData == null) continue;
                if (dinoData.diet != Dinosaur.Diet.HERBIVORE && feeder.hasMeat()) return pos.toImmutable();
                if (dinoData.diet != Dinosaur.Diet.CARNIVORE && feeder.hasVeg()) return pos.toImmutable();
            }
        }
        return null;
    }
}
