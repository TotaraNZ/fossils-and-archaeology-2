package mod.fossilsarch2.dinosaur;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Dinosaur species data. Loaded as a NeoForge custom datapack registry from
 * {@code data/<ns>/dinosaurs/*.json}; each entry's identifier is its filename.
 *
 * <p>The schema splits the 30+ tunable fields into coherent sub-records so
 * the codec stays under {@link RecordCodecBuilder}'s 16-field cap and so
 * consumers can pull related properties together.
 */
public record Dinosaur(
        String displayName,
        Diet diet,
        CombatStats combat,
        GrowthStats growth,
        HungerStats hunger,
        FoodStats food,
        Animations animations,
        List<String> variants) {

    public enum Diet { HERBIVORE, CARNIVORE, OMNIVORE }

    private static final Codec<Diet> DIET_CODEC = Codec.STRING.xmap(Diet::valueOf, Diet::name);

    public static final Codec<Dinosaur> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("display_name").forGetter(Dinosaur::displayName),
            DIET_CODEC.fieldOf("diet").forGetter(Dinosaur::diet),
            CombatStats.CODEC.fieldOf("combat").forGetter(Dinosaur::combat),
            GrowthStats.CODEC.fieldOf("growth").forGetter(Dinosaur::growth),
            HungerStats.CODEC.optionalFieldOf("hunger", HungerStats.DEFAULT).forGetter(Dinosaur::hunger),
            FoodStats.CODEC.fieldOf("food").forGetter(Dinosaur::food),
            Animations.CODEC.optionalFieldOf("animations", Animations.DEFAULT).forGetter(Dinosaur::animations),
            Codec.STRING.listOf().optionalFieldOf("variants", List.of()).forGetter(Dinosaur::variants)
    ).apply(i, Dinosaur::new));

    public record CombatStats(
            double health, double speed, double attackDamage,
            float combatModifier, float confidence,
            float chaseSpeed, float fleeSpeed,
            float minHuntProgress, float playerAggressionProgress, float defensiveProgress,
            float preyRatioMax, float fearRatio) {
        public static final Codec<CombatStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("health").forGetter(CombatStats::health),
                Codec.DOUBLE.fieldOf("speed").forGetter(CombatStats::speed),
                Codec.DOUBLE.fieldOf("attack_damage").forGetter(CombatStats::attackDamage),
                Codec.FLOAT.optionalFieldOf("combat_modifier", 1.0f).forGetter(CombatStats::combatModifier),
                Codec.FLOAT.optionalFieldOf("confidence", 1.0f).forGetter(CombatStats::confidence),
                Codec.FLOAT.optionalFieldOf("chase_speed", 1.5f).forGetter(CombatStats::chaseSpeed),
                Codec.FLOAT.optionalFieldOf("flee_speed", 1.25f).forGetter(CombatStats::fleeSpeed),
                Codec.FLOAT.optionalFieldOf("min_hunt_progress", 0.6f).forGetter(CombatStats::minHuntProgress),
                Codec.FLOAT.optionalFieldOf("player_aggression_progress", 0.85f).forGetter(CombatStats::playerAggressionProgress),
                Codec.FLOAT.optionalFieldOf("defensive_progress", 0.5f).forGetter(CombatStats::defensiveProgress),
                Codec.FLOAT.optionalFieldOf("prey_ratio_max", 0.8f).forGetter(CombatStats::preyRatioMax),
                Codec.FLOAT.optionalFieldOf("fear_ratio", 1.3f).forGetter(CombatStats::fearRatio)
        ).apply(i, CombatStats::new));
    }

    public record GrowthStats(
            float width, float height,
            float babyScale, float adultScale,
            int maxAge, int growTime, int hatchTime) {
        public static final Codec<GrowthStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("width").forGetter(GrowthStats::width),
                Codec.FLOAT.fieldOf("height").forGetter(GrowthStats::height),
                Codec.FLOAT.fieldOf("baby_scale").forGetter(GrowthStats::babyScale),
                Codec.FLOAT.optionalFieldOf("adult_scale", 1.0f).forGetter(GrowthStats::adultScale),
                Codec.INT.fieldOf("max_age").forGetter(GrowthStats::maxAge),
                Codec.INT.fieldOf("grow_time").forGetter(GrowthStats::growTime),
                Codec.INT.fieldOf("hatch_time").forGetter(GrowthStats::hatchTime)
        ).apply(i, GrowthStats::new));
    }

    public record HungerStats(int maxHunger, int hungryThreshold, int hungerDecayRate, float tameChance) {
        public static final HungerStats DEFAULT = new HungerStats(100, 80, 200, 0.33f);
        public static final Codec<HungerStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("max_hunger", 100).forGetter(HungerStats::maxHunger),
                Codec.INT.optionalFieldOf("hungry_threshold", 80).forGetter(HungerStats::hungryThreshold),
                Codec.INT.optionalFieldOf("hunger_decay_rate", 200).forGetter(HungerStats::hungerDecayRate),
                Codec.FLOAT.optionalFieldOf("tame_chance", 0.33f).forGetter(HungerStats::tameChance)
        ).apply(i, HungerStats::new));
    }

    public record FoodStats(
            int meatNutrition, float meatSaturation,
            int cookedMeatNutrition, float cookedMeatSaturation,
            int meatDropCount) {
        public static final Codec<FoodStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("meat_nutrition").forGetter(FoodStats::meatNutrition),
                Codec.FLOAT.fieldOf("meat_saturation").forGetter(FoodStats::meatSaturation),
                Codec.INT.fieldOf("cooked_meat_nutrition").forGetter(FoodStats::cookedMeatNutrition),
                Codec.FLOAT.fieldOf("cooked_meat_saturation").forGetter(FoodStats::cookedMeatSaturation),
                Codec.INT.fieldOf("meat_drop_count").forGetter(FoodStats::meatDropCount)
        ).apply(i, FoodStats::new));
    }

    public record Animations(String attack, List<String> special) {
        public static final Animations DEFAULT = new Animations("bite", List.of());
        public static final Codec<Animations> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("attack", "bite").forGetter(Animations::attack),
                Codec.STRING.listOf().optionalFieldOf("special", List.of()).forGetter(Animations::special)
        ).apply(i, Animations::new));
    }
}
