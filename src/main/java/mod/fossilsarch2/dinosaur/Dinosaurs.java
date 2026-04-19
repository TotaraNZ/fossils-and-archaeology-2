package mod.fossilsarch2.dinosaur;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Custom NeoForge datapack registry for {@link Dinosaur} entries.
 * Files load from {@code data/<datapack_ns>/fossilsarch2/dinosaurs/<id>.json}.
 *
 * <p>Datapacks (and resource packs that ship data) can override or add entries
 * without code. Runtime lookups go through {@code level.registryAccess()
 * .lookupOrThrow(Dinosaurs.REGISTRY_KEY)}.
 */
public final class Dinosaurs {

	public static final ResourceKey<Registry<Dinosaur>> REGISTRY_KEY = ResourceKey.createRegistryKey(
			Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "dinosaurs"));

	public static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(REGISTRY_KEY, Dinosaur.CODEC, Dinosaur.CODEC);
	}

	private Dinosaurs() {}
}
