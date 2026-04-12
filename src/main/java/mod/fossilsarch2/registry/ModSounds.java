package mod.fossilsarch2.registry;

import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.dinosaur.DinosaurUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamically registers sound events for all dinosaur species.
 * Uses the dinosaur's source namespace so addon mods' sounds are registered correctly.
 */
public final class ModSounds {

    private static final Map<String, SoundEvent> ALL = new HashMap<>();

    public static void registerDinosaurSounds() {
        for (var entry : DinosaurRegistry.all().entrySet()) {
            Dinosaur dino = entry.getValue();
            String ns = DinosaurUtils.getNamespace(dino);
            registerSound(ns, dino.id + ".ambient");
            registerSound(ns, dino.id + ".hurt");
            registerSound(ns, dino.id + ".death");
        }
    }

    private static void registerSound(String namespace, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
        ALL.put(path, event);
    }

    public static SoundEvent get(String path) {
        return ALL.get(path);
    }

    public static void init() {
        // Called after dinosaurs are registered
    }

    private ModSounds() {}
}
