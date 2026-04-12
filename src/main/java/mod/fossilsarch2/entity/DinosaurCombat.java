package mod.fossilsarch2.entity;

import java.util.UUID;

import mod.fossilsarch2.dinosaur.Dinosaur;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

public final class DinosaurCombat {

    private DinosaurCombat() {}

    public static DinosaurEntity.GrowthStage getGrowthStage(DinosaurEntity dinosaur) {
        float progress = dinosaur.getGrowthProgress();
        if (progress < 0.25f) return DinosaurEntity.GrowthStage.HATCHLING;
        if (progress < 0.6f) return DinosaurEntity.GrowthStage.JUVENILE;
        if (progress < 0.85f) return DinosaurEntity.GrowthStage.SUBADULT;
        return DinosaurEntity.GrowthStage.ADULT;
    }

    public static double getBodyMass(DinosaurEntity dinosaur) {
        Dinosaur data = dinosaur.getDinosaur();
        if (data == null) return 1.0;
        double scale = dinosaur.getScaleFactor();
        return Math.max(0.1, data.width * data.height * scale * scale);
    }

    public static double getAdultBodyMass(DinosaurEntity dinosaur) {
        Dinosaur data = dinosaur.getDinosaur();
        if (data == null) return 1.0;
        double scale = data.adult_scale;
        return Math.max(0.1, data.width * data.height * scale * scale);
    }

    public static double getCombatPower(DinosaurEntity dinosaur) {
        return CombatEvaluation.of(dinosaur).combatPower();
    }

    public static float getHealthScaleFactor(DinosaurEntity dinosaur) {
        return 0.35f + 0.65f * dinosaur.getGrowthProgress();
    }

    public static float getAttackScaleFactor(DinosaurEntity dinosaur) {
        float progress = dinosaur.getGrowthProgress();
        return 0.05f + 0.95f * progress * progress;
    }

    public static double getKnockbackResistanceForGrowth(DinosaurEntity dinosaur) {
        double adultBodyMass = getAdultBodyMass(dinosaur);
        double massRatio = adultBodyMass > 0.0 ? getBodyMass(dinosaur) / adultBodyMass : 1.0;
        massRatio = Math.max(0.0, Math.min(1.0, massRatio));
        return 0.05 + 0.45 * massRatio;
    }

    public static boolean isDefensivelyMature(DinosaurEntity dinosaur) {
        Dinosaur data = dinosaur.getDinosaur();
        return data != null && dinosaur.getGrowthProgress() >= data.defensive_progress;
    }

    public static boolean shouldHunt(DinosaurEntity dinosaur, LivingEntity target) {
        Dinosaur data = dinosaur.getDinosaur();
        if (data == null || data.diet == Dinosaur.Diet.HERBIVORE) return false;
        if (!isPreyCandidate(target) || isFriendlyOrInvalidTarget(dinosaur, target)) return false;
        if (dinosaur.getGrowthProgress() < data.min_hunt_progress) return false;

        CombatEvaluation self = CombatEvaluation.of(dinosaur);
        CombatEvaluation other = CombatEvaluation.of(target);
        double preyThreshold = data.prey_ratio_max * Math.max(0.35f, data.confidence);
        return other.combatPower() <= self.combatPower() * preyThreshold;
    }

    public static boolean shouldAttackPlayer(DinosaurEntity dinosaur, Player target) {
        Dinosaur data = dinosaur.getDinosaur();
        if (data == null || data.diet == Dinosaur.Diet.HERBIVORE) return false;
        if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()) return false;
        if (dinosaur.isTame() || !dinosaur.isHungry()) return false;
        if (isFriendlyOrInvalidTarget(dinosaur, target)) return false;
        if (dinosaur.getGrowthProgress() < data.player_aggression_progress) return false;
        if (shouldFear(dinosaur, target)) return false;

        CombatEvaluation self = CombatEvaluation.of(dinosaur);
        CombatEvaluation other = CombatEvaluation.of(target);
        double preyThreshold = data.prey_ratio_max * Math.max(0.35f, data.confidence) * 0.9;
        return other.combatPower() <= self.combatPower() * preyThreshold;
    }

    public static boolean shouldFear(DinosaurEntity dinosaur, LivingEntity target) {
        Dinosaur data = dinosaur.getDinosaur();
        if (data == null || target == null || !target.isAlive()) return false;
        if (isFriendlyOrInvalidTarget(dinosaur, target)) return false;
        if (isDefensivelyMature(dinosaur) && (dinosaur.getTarget() == target || dinosaur.getLastHurtByMob() == target)) {
            return false;
        }
        if (shouldHunt(dinosaur, target)) return false;

        CombatEvaluation self = CombatEvaluation.of(dinosaur);
        CombatEvaluation other = CombatEvaluation.of(target);
        if (other.avoidAlways()) return true;

        double fearThreshold = data.fear_ratio / Math.max(0.35f, data.confidence);
        if (data.diet == Dinosaur.Diet.HERBIVORE && !isDefensivelyMature(dinosaur)) {
            fearThreshold *= 0.8;
        }

        return other.combatPower() >= self.combatPower() * fearThreshold;
    }

    private static boolean isFriendlyOrInvalidTarget(DinosaurEntity dinosaur, LivingEntity target) {
        if (target == dinosaur || !target.isAlive()) {
            return true;
        }

        if (target instanceof Player player && dinosaur.getOwner() != null && dinosaur.getOwner().is(player)) {
            return true;
        }

        UUID ownerUuid = dinosaur.getOwnerReference() != null ? dinosaur.getOwnerReference().getUUID() : null;
        if (ownerUuid != null && target instanceof TamableAnimal tamable && tamable.isTame()
                && tamable.getOwnerReference() != null
                && ownerUuid.equals(tamable.getOwnerReference().getUUID())) {
            return true;
        }

        if (target instanceof DinosaurEntity other) {
            if (other.getDinosaurId().equals(dinosaur.getDinosaurId())) {
                return true;
            }

            if (ownerUuid != null && other.isTame() && other.getOwnerReference() != null
                    && ownerUuid.equals(other.getOwnerReference().getUUID())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPreyCandidate(LivingEntity target) {
        // DinosaurEntity extends TamableAnimal -> Animal, so this intentionally includes
        // dinosaur-vs-dinosaur predation unless the target is filtered out elsewhere.
        return target != null && target.isAlive() && target instanceof Animal;
    }
}
