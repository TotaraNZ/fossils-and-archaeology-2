package mod.fossilsarch2.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;

public record CombatEvaluation(double bodyMass, double combatPower, boolean avoidAlways) {

    private static final double MIN_MASS = 0.1;
    private static final double MIN_POWER = 0.1;

    public static CombatEvaluation of(LivingEntity entity) {
        if (entity instanceof DinosaurEntity dinosaur) {
            return fromDinosaur(dinosaur);
        }

        double width = Math.max(0.1, entity.getBbWidth());
        double height = Math.max(0.1, entity.getBbHeight());
        double bodyMass = Math.max(MIN_MASS, width * height);
        double healthFraction = Math.max(0.1, entity.getHealth() / Math.max(1.0, entity.getMaxHealth()));
        double attackDamage = getAttackDamage(entity);
        if (attackDamage <= 0.0) {
            attackDamage = fallbackAttackDamage(entity);
        }

        boolean avoidAlways = entity instanceof Creeper;
        double threatModifier = fallbackThreatModifier(entity);
        double combatPower = Math.max(MIN_POWER, bodyMass * Math.max(0.1, attackDamage) * healthFraction * threatModifier);
        return new CombatEvaluation(bodyMass, combatPower, avoidAlways);
    }

    private static CombatEvaluation fromDinosaur(DinosaurEntity dinosaur) {
        double bodyMass = Math.max(MIN_MASS, dinosaur.getBodyMass());
        double healthFraction = Math.max(0.1, dinosaur.getHealth() / Math.max(1.0, dinosaur.getMaxHealth()));
        double attackDamage = Math.max(0.1, getAttackDamage(dinosaur));
        double modifier = dinosaur.getDinosaur() != null ? dinosaur.getDinosaur().combat_modifier : 1.0f;
        double combatPower = Math.max(MIN_POWER, bodyMass * attackDamage * healthFraction * modifier);
        return new CombatEvaluation(bodyMass, combatPower, false);
    }

    private static double getAttackDamage(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        return attribute != null ? attribute.getValue() : 0.0;
    }

    private static double fallbackAttackDamage(LivingEntity entity) {
        if (entity instanceof IronGolem) return 15.0;
        if (entity instanceof Ravager) return 12.0;
        if (entity instanceof Monster) return 4.0;
        if (entity instanceof Player) return 3.0;
        if (entity instanceof Animal) return 2.0;
        return 1.0;
    }

    private static double fallbackThreatModifier(LivingEntity entity) {
        if (entity instanceof IronGolem || entity instanceof Ravager) return 2.5;
        if (entity instanceof Creeper) return 1.75;
        if (entity instanceof Monster) return 1.35;
        if (entity instanceof Player) return 1.2;
        return 1.0;
    }
}
