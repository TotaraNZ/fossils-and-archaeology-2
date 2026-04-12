package mod.fossilsarch2.dinosaur;

import mod.fossilsarch2.registry.DinosaurRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Centralized species ID extraction. Every system that needs to derive a species
 * from an item, entity type, or string should go through here.
 */
public final class DinosaurUtils {

    private static final String[] ITEM_SUFFIXES = {"_cooked_meat", "_meat", "_dna", "_egg"};

    /**
     * Extract the species ID from an item stack's registry path.
     * e.g. "tyrannosaurus_meat" -> "tyrannosaurus", "tyrannosaurus_cooked_meat" -> "tyrannosaurus"
     * Returns null if the item doesn't match any known dinosaur item pattern.
     */
    public static String getSpeciesFromItem(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return getSpeciesFromPath(id.getPath());
    }

    /**
     * Extract the species ID from a registry path string.
     * Strips known suffixes in order (longest first to avoid partial matches).
     */
    public static String getSpeciesFromPath(String path) {
        for (String suffix : ITEM_SUFFIXES) {
            if (path.endsWith(suffix)) {
                String species = path.substring(0, path.length() - suffix.length());
                if (!species.isEmpty()) return species;
            }
        }
        return null;
    }

    /**
     * Look up a Dinosaur by species ID string, checking all registered namespaces.
     */
    public static Dinosaur getBySpeciesId(String speciesId) {
        for (var entry : DinosaurRegistry.all().entrySet()) {
            if (entry.getKey().getPath().equals(speciesId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Get the namespace (mod ID) that registered a given dinosaur.
     */
    public static String getNamespace(Dinosaur dino) {
        return dino.namespace != null ? dino.namespace : "fossilsarch2";
    }

    private DinosaurUtils() {}
}
