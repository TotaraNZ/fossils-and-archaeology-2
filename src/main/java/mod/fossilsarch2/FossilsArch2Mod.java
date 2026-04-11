package mod.fossilsarch2;

import org.slf4j.Logger;

import mod.fossilsarch2.data.DinosaurJsonLoader;
import mod.fossilsarch2.network.DinopediaPayload;
import mod.fossilsarch2.registry.DinosaurRegistry;
import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.registry.ModBlocks;
import mod.fossilsarch2.registry.ModEntities;
import mod.fossilsarch2.registry.ModItemGroups;
import mod.fossilsarch2.registry.ModItems;
import mod.fossilsarch2.registry.ModScreenHandlers;
import mod.fossilsarch2.registry.ModSounds;
import mod.fossilsarch2.world.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;

public class FossilsArch2Mod implements ModInitializer {

	public static final String MOD_ID = "fossilsarch2";
	public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		DinosaurJsonLoader.load().forEach(d -> DinosaurRegistry.register(
				Identifier.of(d.namespace != null ? d.namespace : MOD_ID, d.id), d));

		ModBlocks.init();
		ModBlockEntities.init();
		ModScreenHandlers.init();

		if (!DinosaurRegistry.all().isEmpty()) {
			ModItems.registerDinosaurItems();
			ModEntities.registerDinosaurEntities();
			ModSounds.registerDinosaurSounds();

			LOGGER.info("Fossils & Archaeology 2 initialized with {} dinosaurs.", DinosaurRegistry.all().size());
		} else {
			LOGGER.warn("Fossils & Archaeology 2 initialized but no dinosaurs were loaded.");
		}

		ModWorldGeneration.register();
		ModItemGroups.init();

		// Register network payloads
		PayloadTypeRegistry.playS2C().register(DinopediaPayload.ID, DinopediaPayload.CODEC);
	}
}
