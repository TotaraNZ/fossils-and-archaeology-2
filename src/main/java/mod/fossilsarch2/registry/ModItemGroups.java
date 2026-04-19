package mod.fossilsarch2.registry;

import java.util.HashSet;
import java.util.Set;

import mod.fossilsarch2.FossilsArch2Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative tab that automatically includes ALL items and blocks from the fossilsarch2 namespace.
 */
public final class ModItemGroups {

	private static final DeferredRegister<CreativeModeTab> TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FossilsArch2Mod.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FA2 = TABS.register("fossilsarch2",
			() -> CreativeModeTab.builder()
					.icon(() -> new ItemStack(ModItems.SCARAB_GEM.get()))
					.title(Component.translatable("itemGroup.fossilsarch2"))
					.displayItems((context, entries) -> {
						Set<Item> added = new HashSet<>();
						BuiltInRegistries.BLOCK.forEach(block -> {
							Identifier id = BuiltInRegistries.BLOCK.getKey(block);
							if (id.getNamespace().equals(FossilsArch2Mod.MOD_ID)) {
								Item blockItem = block.asItem();
								if (blockItem != null && added.add(blockItem)) {
									entries.accept(blockItem);
								}
							}
						});
						BuiltInRegistries.ITEM.forEach(item -> {
							Identifier id = BuiltInRegistries.ITEM.getKey(item);
							if (id.getNamespace().equals(FossilsArch2Mod.MOD_ID) && added.add(item)) {
								entries.accept(item);
							}
						});
					})
					.build());

	public static void register(IEventBus modEventBus) {
		TABS.register(modEventBus);
	}

	private ModItemGroups() {}
}
