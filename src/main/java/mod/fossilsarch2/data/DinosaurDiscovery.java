package mod.fossilsarch2.data;

import java.io.BufferedReader;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import net.neoforged.fml.ModContainer;

/**
 * Construct-time scan of the mod's own jar to read dinosaur definitions.
 * Items, entities, and sounds derived from a dinosaur (DNA, egg, meat, sounds)
 * must register before any datapack loads, so the {@code @Mod} constructor
 * needs the {@link Dinosaur} records before the datapack registry has fired.
 *
 * <p>The same files are then loaded properly by NeoForge's datapack registry
 * machinery (see {@link mod.fossilsarch2.dinosaur.Dinosaurs}); this scan is
 * the bootstrap fallback that makes per-dinosaur item registration possible.
 */
public final class DinosaurDiscovery {

	// Custom datapack registries live under data/<ns>/<modid>/<registryname>/.
	private static final String DINO_DIR = "data/" + FossilsArch2Mod.MOD_ID
			+ "/" + FossilsArch2Mod.MOD_ID + "/dinosaurs";

	private DinosaurDiscovery() {}

	public static Map<String, Dinosaur> discoverFromMod(ModContainer container) {
		Map<String, Dinosaur> result = new LinkedHashMap<>();
		var jar = container.getModInfo().getOwningFile().getFile().getContents();
		jar.visitContent(DINO_DIR, (path, resource) -> {
			if (!path.endsWith(".json")) return;
			String fileName = path.substring(path.lastIndexOf('/') + 1);
			String id = fileName.substring(0, fileName.length() - ".json".length());
			try (BufferedReader reader = resource.bufferedReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				Dinosaur dinosaur = Dinosaur.CODEC.parse(JsonOps.INSTANCE, json)
						.resultOrPartial(error -> FossilsArch2Mod.LOGGER.error(
								"Failed to parse dinosaur '{}': {}", id, error))
						.orElse(null);
				if (dinosaur != null) {
					result.put(id, dinosaur);
				}
			} catch (Exception e) {
				FossilsArch2Mod.LOGGER.error("Error reading dinosaur JSON: {}", path, e);
			}
		});
		return result;
	}
}
