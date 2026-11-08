package mod.fossilsarch2.dinosaur;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

/**
 * Dinosaur species data. Loaded as a NeoForge custom datapack registry from
 * {@code data/<ns>/fossilsarch2/dinosaurs/*.json}; each entry's identifier is its filename.
 *
 * <p>The schema splits the 30+ tunable fields into coherent sub-records so
 * the codec stays under {@link RecordCodecBuilder}'s 16-field cap and so
 * consumers can pull related properties together.
 *
 * <p>Every numeric field is range-checked by its codec, so a bad datapack value is reported
 * as a parse error when the datapack loads instead of crashing the server tick later.
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

    public enum Diet implements StringRepresentable {
        HERBIVORE("herbivore"),
        CARNIVORE("carnivore"),
        OMNIVORE("omnivore");

        public static final Codec<Diet> CODEC = StringRepresentable.fromEnum(Diet::values);

        private final String serializedName;

        Diet(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public boolean eatsMeat() {
            return this != HERBIVORE;
        }

        public boolean eatsPlants() {
            return this != CARNIVORE;
        }
    }

    private static final Codec<Double> NON_NEGATIVE_DOUBLE = Codec.doubleRange(0.0, Double.MAX_VALUE);
    private static final Codec<Float> UNIT_FLOAT = Codec.floatRange(0.0f, 1.0f);

    public static final Codec<Dinosaur> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("display_name").forGetter(Dinosaur::displayName),
            Diet.CODEC.fieldOf("diet").forGetter(Dinosaur::diet),
            CombatStats.CODEC.fieldOf("combat").forGetter(Dinosaur::combat),
            GrowthStats.CODEC.fieldOf("growth").forGetter(Dinosaur::growth),
            HungerStats.CODEC.optionalFieldOf("hunger", HungerStats.DEFAULT).forGetter(Dinosaur::hunger),
            FoodStats.CODEC.fieldOf("food").forGetter(Dinosaur::food),
            Animations.CODEC.optionalFieldOf("animations", Animations.DEFAULT).forGetter(Dinosaur::animations),
            Codec.STRING.listOf().optionalFieldOf("variants", List.of()).forGetter(Dinosaur::variants))
            .apply(i, Dinosaur::new));

    public record CombatStats(
            double health, double speed, double attackDamage,
            float combatModifier, float confidence,
            float chaseSpeed, float fleeSpeed,
            float minHuntProgress, float playerAggressionProgress, float defensiveProgress,
            float preyRatioMax, float fearRatio) {
        public static final Codec<CombatStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(1.0, Double.MAX_VALUE).fieldOf("health").forGetter(CombatStats::health),
                NON_NEGATIVE_DOUBLE.fieldOf("speed").forGetter(CombatStats::speed),
                NON_NEGATIVE_DOUBLE.fieldOf("attack_damage").forGetter(CombatStats::attackDamage),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("combat_modifier", 1.0f)
                        .forGetter(CombatStats::combatModifier),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("confidence", 1.0f).forGetter(CombatStats::confidence),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("chase_speed", 1.5f).forGetter(CombatStats::chaseSpeed),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("flee_speed", 1.25f).forGetter(CombatStats::fleeSpeed),
                UNIT_FLOAT.optionalFieldOf("min_hunt_progress", 0.6f).forGetter(CombatStats::minHuntProgress),
                UNIT_FLOAT.optionalFieldOf("player_aggression_progress", 0.85f)
                        .forGetter(CombatStats::playerAggressionProgress),
                UNIT_FLOAT.optionalFieldOf("defensive_progress", 0.5f).forGetter(CombatStats::defensiveProgress),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("prey_ratio_max", 0.8f).forGetter(CombatStats::preyRatioMax),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("fear_ratio", 1.3f).forGetter(CombatStats::fearRatio))
                .apply(i, CombatStats::new));
    }

    public record GrowthStats(
            float width, float height,
            float babyScale, float adultScale,
            int maxAge, int growTime, int hatchTime) {
        public static final Codec<GrowthStats> CODEC = RecordCodecBuilder.<GrowthStats>create(i -> i.group(
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("width").forGetter(GrowthStats::width),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("height").forGetter(GrowthStats::height),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("baby_scale").forGetter(GrowthStats::babyScale),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("adult_scale", 1.0f).forGetter(GrowthStats::adultScale),
                ExtraCodecs.POSITIVE_INT.fieldOf("max_age").forGetter(GrowthStats::maxAge),
                ExtraCodecs.POSITIVE_INT.fieldOf("grow_time").forGetter(GrowthStats::growTime),
                ExtraCodecs.POSITIVE_INT.fieldOf("hatch_time").forGetter(GrowthStats::hatchTime))
                .apply(i, GrowthStats::new)).validate(g -> g.babyScale() <= g.adultScale()
                        ? DataResult.success(g)
                        : DataResult.error(() -> "baby_scale (" + g.babyScale() + ") must not exceed adult_scale ("
                                + g.adultScale() + ")"));
    }

    /**
     * Hunger tuning. A dinosaur seeks food below {@code hungry_threshold}, and a predator only hunts
     * live prey below the lower {@code hunt_threshold}, so feeders and ferns are used first.
     */
    public record HungerStats(int maxHunger, int hungryThreshold, int huntThreshold, int hungerDecayRate,
            float tameChance) {
        public static final HungerStats DEFAULT = new HungerStats(100, 80, 40, 200, 0.33f);
        public static final Codec<HungerStats> CODEC = RecordCodecBuilder.<HungerStats>create(i -> i.group(
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_hunger", DEFAULT.maxHunger())
                        .forGetter(HungerStats::maxHunger),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("hungry_threshold", DEFAULT.hungryThreshold())
                        .forGetter(HungerStats::hungryThreshold),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("hunt_threshold", DEFAULT.huntThreshold())
                        .forGetter(HungerStats::huntThreshold),
                // 0 disables hunger decay entirely
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("hunger_decay_rate", DEFAULT.hungerDecayRate())
                        .forGetter(HungerStats::hungerDecayRate),
                UNIT_FLOAT.optionalFieldOf("tame_chance", DEFAULT.tameChance()).forGetter(HungerStats::tameChance))
                .apply(i, HungerStats::new)).validate(HungerStats::validate);

        private static DataResult<HungerStats> validate(HungerStats h) {
            if (h.hungryThreshold() > h.maxHunger()) {
                return DataResult.error(() -> "hungry_threshold (" + h.hungryThreshold()
                        + ") must not exceed max_hunger (" + h.maxHunger() + ")");
            }
            if (h.huntThreshold() > h.hungryThreshold()) {
                return DataResult.error(() -> "hunt_threshold (" + h.huntThreshold()
                        + ") must not exceed hungry_threshold (" + h.hungryThreshold() + ")");
            }
            return DataResult.success(h);
        }
    }

    public record FoodStats(
            int meatNutrition, float meatSaturation,
            int cookedMeatNutrition, float cookedMeatSaturation,
            int meatDropCount) {
        public static final Codec<FoodStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("meat_nutrition").forGetter(FoodStats::meatNutrition),
                ExtraCodecs.NON_NEGATIVE_FLOAT.fieldOf("meat_saturation").forGetter(FoodStats::meatSaturation),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooked_meat_nutrition").forGetter(FoodStats::cookedMeatNutrition),
                ExtraCodecs.NON_NEGATIVE_FLOAT.fieldOf("cooked_meat_saturation")
                        .forGetter(FoodStats::cookedMeatSaturation),
                // Drops 1..meat_drop_count meat; 0 disables the drop
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("meat_drop_count").forGetter(FoodStats::meatDropCount))
                .apply(i, FoodStats::new));
    }

    /**
     * GeckoLib animation names for this species. Every name refers to an animation in the
     * species' {@code geckolib/animations/<id>.animation.json}; an empty string disables that slot.
     */
    public record Animations(String walk, String idle, String attack, String eat, List<String> special) {
        public static final Animations DEFAULT = new Animations("walk", "idle", "bite", "eat", List.of());
        public static final Codec<Animations> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("walk", DEFAULT.walk()).forGetter(Animations::walk),
                Codec.STRING.optionalFieldOf("idle", DEFAULT.idle()).forGetter(Animations::idle),
                Codec.STRING.optionalFieldOf("attack", DEFAULT.attack()).forGetter(Animations::attack),
                Codec.STRING.optionalFieldOf("eat", DEFAULT.eat()).forGetter(Animations::eat),
                Codec.STRING.listOf().optionalFieldOf("special", List.of()).forGetter(Animations::special))
                .apply(i, Animations::new));
    }
}
