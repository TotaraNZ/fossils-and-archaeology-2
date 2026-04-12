package mod.fossilsarch2.registry;

import java.util.HashSet;
import java.util.Set;

import mod.fossilsarch2.FossilsArch2Mod;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Creative tab that automatically includes ALL items and blocks from the fossilsarch2 namespace.
 * No manual item list — adding new items/blocks to the mod automatically adds them here.
 */
public final class ModItemGroups {

    public static final ResourceKey<CreativeModeTab> FA2_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "fossilsarch2"));

    public static final CreativeModeTab FA2 = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, FA2_KEY,
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SCARAB_GEM))
                    .title(Component.translatable("itemGroup.fossilsarch2"))
                    .displayItems((context, entries) -> {
                        Set<Item> added = new HashSet<>();

                        // Add all block items first (machines, decorative blocks)
                        BuiltInRegistries.BLOCK.forEach(block -> {
                            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                            if (id.getNamespace().equals(FossilsArch2Mod.MOD_ID)) {
                                Item blockItem = block.asItem();
                                if (blockItem != null && added.add(blockItem)) {
                                    entries.accept(blockItem);
                                }
                            }
                        });

                        // Add all remaining items (tools, materials, dino items)
                        BuiltInRegistries.ITEM.forEach(item -> {
                            Identifier id = BuiltInRegistries.ITEM.getKey(item);
                            if (id.getNamespace().equals(FossilsArch2Mod.MOD_ID) && added.add(item)) {
                                entries.accept(item);
                            }
                        });
                    })
                    .build());

    public static void init() {
        // Static init triggers registration
    }

    private ModItemGroups() {}
}
