package mod.fossilsarch2;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import mod.fossilsarch2.data.DinosaurDiscovery;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.dinosaur.Dinosaurs;
import mod.fossilsarch2.network.ModNetworking;
import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.registry.ModBlocks;
import mod.fossilsarch2.registry.ModEntities;
import mod.fossilsarch2.registry.ModItemGroups;
import mod.fossilsarch2.registry.ModItems;
import mod.fossilsarch2.registry.ModScreenHandlers;
import mod.fossilsarch2.registry.ModSounds;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

@Mod(FossilsArch2Mod.MOD_ID)
public class FossilsArch2Mod {

	public static final String MOD_ID = "fossilsarch2";
	public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

	public FossilsArch2Mod(IEventBus modEventBus, ModContainer container) {
		// Discover dinosaur IDs from this mod's own jar at construct time so
		// items/entities/sounds can be registered up-front. The Dinosaur data
		// objects themselves populate later via the datapack registry.
		Map<String, Dinosaur> dinosaurs = DinosaurDiscovery.discoverFromMod(container);
		LOGGER.info("Discovered {} dinosaur definitions in {}.", dinosaurs.size(), MOD_ID);

		ModBlocks.register(modEventBus);
		ModItems.register(modEventBus, dinosaurs);
		ModBlockEntities.register(modEventBus);
		ModEntities.register(modEventBus, dinosaurs);
		ModSounds.register(modEventBus, dinosaurs);
		ModScreenHandlers.register(modEventBus);
		ModItemGroups.register(modEventBus);

		modEventBus.addListener(ModEntities::onEntityAttributes);
		modEventBus.addListener(ModNetworking::onRegisterPayloadHandlers);
		modEventBus.addListener(FossilsArch2Mod::onRegisterCapabilities);
		modEventBus.addListener(Dinosaurs::onNewRegistry);
	}

	private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		// Expose machine inventories so hoppers, droppers, and modded pipes can interact with them.
		List<DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>>> machineTypes = List.of(
				ModBlockEntities.ANALYSER,
				ModBlockEntities.CULTIVATOR,
				ModBlockEntities.FEEDER,
				ModBlockEntities.WORKTABLE);
		for (var holder : machineTypes) {
			event.registerBlockEntity(
					Capabilities.Item.BLOCK,
					holder.get(),
					(be, side) -> VanillaContainerWrapper.of((Container) be));
		}
	}
}
