package mod.fossilsarch2.dinosaur;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Centralized species ID extraction and runtime registry lookups.
 * Every system that needs to derive a species from an item, entity type, or
 * string should go through here.
 */
public final class DinosaurUtils {

	private static final String[] ITEM_SUFFIXES = {"_cooked_meat", "_meat", "_dna", "_egg"};

	/**
	 * Extract the species ID from an item stack's registry path.
	 * e.g. "tyrannosaurus_meat" -&gt; "tyrannosaurus".
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
	 * Look up a Dinosaur by species ID via the level's registry access.
	 * Returns null if the entry isn't present in any loaded datapack.
	 */
	public static Dinosaur getBySpeciesId(Level level, String speciesId) {
		Identifier id = Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, speciesId);
		Holder.Reference<Dinosaur> ref = level.registryAccess()
				.lookupOrThrow(Dinosaurs.REGISTRY_KEY)
				.get(id)
				.orElse(null);
		return ref != null ? ref.value() : null;
	}

	private DinosaurUtils() {}
}
