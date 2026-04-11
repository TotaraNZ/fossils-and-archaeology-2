package mod.fossilsarch2.registry;

import java.util.HashSet;
import java.util.Set;

import mod.fossilsarch2.FossilsArch2Mod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Creative tab that automatically includes ALL items and blocks from the fossilsarch2 namespace.
 * No manual item list — adding new items/blocks to the mod automatically adds them here.
 */
public final class ModItemGroups {

    public static final RegistryKey<ItemGroup> FA2_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP, Identifier.of(FossilsArch2Mod.MOD_ID, "fossilsarch2"));

    public static final ItemGroup FA2 = Registry.register(
            Registries.ITEM_GROUP, FA2_KEY,
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.SCARAB_GEM))
                    .displayName(Text.translatable("itemGroup.fossilsarch2"))
                    .entries((context, entries) -> {
                        Set<Item> added = new HashSet<>();

                        // Add all block items first (machines, decorative blocks)
                        Registries.BLOCK.forEach(block -> {
                            Identifier id = Registries.BLOCK.getId(block);
                            if (id.getNamespace().equals(FossilsArch2Mod.MOD_ID)) {
                                Item blockItem = block.asItem();
                                if (blockItem != null && added.add(blockItem)) {
                                    entries.add(blockItem);
                                }
                            }
                        });

                        // Add all remaining items (tools, materials, dino items)
                        Registries.ITEM.forEach(item -> {
                            Identifier id = Registries.ITEM.getId(item);
                            if (id.getNamespace().equals(FossilsArch2Mod.MOD_ID) && added.add(item)) {
                                entries.add(item);
                            }
                        });
                    })
                    .build());

    public static void init() {
        // Static init triggers registration
    }

    private ModItemGroups() {}
}
