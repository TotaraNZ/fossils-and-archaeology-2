package mod.fossilsarch2.entity.ai;

import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class DinoUseFeederGoal extends Goal {

    private final DinosaurEntity dino;
    private BlockPos feederPos;
    private int cooldown = 0;

    public DinoUseFeederGoal(DinosaurEntity dino) {
        this.dino = dino;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        if (!dino.isHungry()) return false;

        feederPos = findNearbyFeeder();
        if (feederPos == null) {
            cooldown = 60;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return dino.isHungry() && feederPos != null;
    }

    @Override
    public void start() {
        dino.getNavigation().moveTo(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (feederPos == null) return;

        double dist = dino.distanceToSqr(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5);
        if (dist < 4.0) {
            // Close enough to eat
            Level level = dino.level();
            if (level.getBlockEntity(feederPos) instanceof FeederBlockEntity feeder) {
                Dinosaur dinoData = dino.getDinosaur();
                boolean fed = false;

                if (dinoData != null && dinoData.diet() != Dinosaur.Diet.HERBIVORE && feeder.hasMeat()) {
                    feeder.consumeMeat(20);
                    dino.feed(20);
                    fed = true;
                }
                if (dinoData != null && dinoData.diet() != Dinosaur.Diet.CARNIVORE && feeder.hasVeg()) {
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
            dino.getNavigation().moveTo(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        feederPos = null;
        cooldown = 200;
    }

    private BlockPos findNearbyFeeder() {
        BlockPos dinoPos = dino.blockPosition();
        Level level = dino.level();

        for (BlockPos pos : BlockPos.withinManhattan(dinoPos, 16, 4, 16)) {
            if (level.getBlockEntity(pos) instanceof FeederBlockEntity feeder) {
                Dinosaur dinoData = dino.getDinosaur();
                if (dinoData == null) continue;
                if (dinoData.diet() != Dinosaur.Diet.HERBIVORE && feeder.hasMeat()) return pos.immutable();
                if (dinoData.diet() != Dinosaur.Diet.CARNIVORE && feeder.hasVeg()) return pos.immutable();
            }
        }
        return null;
    }
}
