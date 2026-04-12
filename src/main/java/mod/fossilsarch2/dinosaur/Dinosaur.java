package mod.fossilsarch2.dinosaur;

import java.util.List;

public class Dinosaur {

    public enum Diet {
        HERBIVORE, CARNIVORE, OMNIVORE
    }

    // Required fields
    public String id;
    public String display_name;
    public double health;
    public double speed;
    public double attack_damage;
    public float width;
    public float height;
    public float baby_scale;
    public float adult_scale = 1.0f;
    public float combat_modifier = 1.0f;
    public float confidence = 1.0f;
    public Diet diet;
    public int max_age;
    public int grow_time;
    public int hatch_time;
    public int meat_nutrition;
    public float meat_saturation;
    public int cooked_meat_nutrition;
    public float cooked_meat_saturation;
    public int meat_drop_count;
    public List<String> variants;

    // Optional tuning fields — defaults applied after deserialization
    public int max_hunger = 100;
    public int hungry_threshold = 80;
    public int hunger_decay_rate = 200;   // ticks between hunger decrements
    public float tame_chance = 0.33f;     // probability per feeding attempt
    public float chase_speed = 1.5f;      // speed multiplier when attacking
    public float flee_speed = 1.25f;      // speed multiplier when fleeing
    public float min_hunt_progress = 0.6f;
    public float player_aggression_progress = 0.85f;
    public float defensive_progress = 0.5f;
    public float prey_ratio_max = 0.8f;
    public float fear_ratio = 1.3f;
    public String attack_animation = "bite";
    public List<String> special_animations;  // e.g. ["sniff", "look"] — played randomly when idle

    // Set after loading — not from JSON
    public transient String namespace;

    public Dinosaur() {
    }

    /**
     * Validates required fields after deserialization.
     * Returns null if valid, or an error message string if invalid.
     */
    public String validate() {
        if (id == null || id.isEmpty()) return "missing 'id'";
        if (display_name == null || display_name.isEmpty()) return "missing 'display_name'";
        if (health <= 0) return "health must be > 0";
        if (speed <= 0) return "speed must be > 0";
        if (width <= 0 || height <= 0) return "width and height must be > 0";
        if (diet == null) return "missing 'diet' (HERBIVORE, CARNIVORE, or OMNIVORE)";
        if (max_age <= 0) return "max_age must be > 0";
        if (grow_time <= 0) return "grow_time must be > 0";
        if (hatch_time <= 0) return "hatch_time must be > 0";
        if (baby_scale <= 0 || baby_scale > 1) return "baby_scale must be between 0 and 1";
        if (adult_scale <= 0) return "adult_scale must be > 0";
        if (adult_scale < baby_scale) return "adult_scale must be >= baby_scale";
        if (combat_modifier <= 0) return "combat_modifier must be > 0";
        if (confidence <= 0) return "confidence must be > 0";
        if (min_hunt_progress < 0 || min_hunt_progress > 1) return "min_hunt_progress must be between 0 and 1";
        if (player_aggression_progress < 0 || player_aggression_progress > 1) return "player_aggression_progress must be between 0 and 1";
        if (defensive_progress < 0 || defensive_progress > 1) return "defensive_progress must be between 0 and 1";
        if (prey_ratio_max <= 0) return "prey_ratio_max must be > 0";
        if (fear_ratio <= 0) return "fear_ratio must be > 0";
        return null;
    }
}
