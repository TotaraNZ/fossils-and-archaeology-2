package mod.fossilsarch2.dinosaur;

import java.util.Optional;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Centralized species ID extraction and runtime registry lookups.
 * Every system that needs to derive a species from an item, entity type, or
 * string should go through here.
 *
 * <p>Species are identified by full {@link Identifier}s (e.g. {@code fossilsarch2:triceratops}),
 * so addon mods can contribute species under their own namespace. The species ID, its
 * entity type ID and the prefix of its items ({@code <ns>:<path>_egg} etc.) all share the
 * same namespace and path.
 */
public final class DinosaurUtils {

	private static final String[] ITEM_SUFFIXES = {"_cooked_meat", "_meat", "_dna", "_egg"};

	/**
	 * Extract the species ID from an item stack's registry ID.
	 * e.g. {@code fossilsarch2:tyrannosaurus_meat} -> {@code fossilsarch2:tyrannosaurus}.
	 * Returns null if the item doesn't match any known dinosaur item pattern.
	 */
	public static @Nullable Identifier getSpeciesFromItem(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String species = getSpeciesFromPath(id.getPath());
		return species != null ? id.withPath(species) : null;
	}

	/**
	 * Extract the species path from a registry path string.
	 * Strips known suffixes in order (longest first to avoid partial matches).
	 */
	public static @Nullable String getSpeciesFromPath(String path) {
		for (String suffix : ITEM_SUFFIXES) {
			if (path.endsWith(suffix)) {
				String species = path.substring(0, path.length() - suffix.length());
				if (!species.isEmpty()) return species;
			}
		}
		return null;
	}

	/**
	 * Parse a species ID, defaulting to this mod's namespace when none is given so that
	 * pre-namespace saves ({@code "triceratops"}) keep loading.
	 */
	public static @Nullable Identifier parseSpeciesId(String id) {
		if (id.isEmpty()) return null;
		return id.indexOf(':') >= 0 ? Identifier.tryParse(id) : Identifier.tryBuild(FossilsArch2Mod.MOD_ID, id);
	}

	/** Look up a species holder via the level's registry access. */
	public static Optional<Holder.Reference<Dinosaur>> getHolder(Level level, Identifier speciesId) {
		return level.registryAccess().lookupOrThrow(Dinosaurs.REGISTRY_KEY).get(speciesId);
	}

	/**
	 * Look up a Dinosaur by species ID via the level's registry access.
	 * Returns null if the entry isn't present in any loaded datapack.
	 */
	public static @Nullable Dinosaur get(Level level, @Nullable Identifier speciesId) {
		return speciesId == null ? null : getHolder(level, speciesId).map(Holder::value).orElse(null);
	}

	private DinosaurUtils() {}
}
