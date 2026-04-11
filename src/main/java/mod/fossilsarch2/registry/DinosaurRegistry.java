package mod.fossilsarch2.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import net.minecraft.util.Identifier;

public class DinosaurRegistry {
    private static final Map<Identifier, Dinosaur> ALL = new HashMap<>();

    public static void register(Identifier id, Dinosaur dinosaur) {
        if (ALL.containsKey(id)) {
            FossilsArch2Mod.LOGGER.warn("Dinosaur with ID {} is already registered. Ignoring.", id);
            return;
        }

        ALL.put(id, dinosaur);
    }

    public static Dinosaur get(Identifier id) {
        return ALL.get(id);
    }

    public static Map<Identifier, Dinosaur> all() {
        return Collections.unmodifiableMap(ALL);
    }
}
