package mod.fossilsarch2.entity.ai;

import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.entity.DinosaurEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class DinoDefensiveMeleeGoal extends MeleeAttackGoal {

    private final DinosaurEntity dino;

    public DinoDefensiveMeleeGoal(DinosaurEntity dino, double speedModifier) {
        super(dino, speedModifier, false);
        this.dino = dino;
    }

    @Override
    public boolean canUse() {
        return canDefend() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return canDefend() && super.canContinueToUse();
    }

    private boolean canDefend() {
        Dinosaur dinosaur = dino.getDinosaur();
        LivingEntity target = dino.getTarget();
        return dinosaur != null
                && dinosaur.diet == Dinosaur.Diet.HERBIVORE
                && dino.isDefensivelyMature()
                && target != null
                && target.isAlive();
    }
}
