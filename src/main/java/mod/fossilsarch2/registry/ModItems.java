package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.item.DinoEggItem;
import mod.fossilsarch2.item.ItemDinopedia;
import mod.fossilsarch2.item.ScarabGemMaterial;
import mod.fossilsarch2.item.TooltipItem;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;

public final class ModItems {

    public static final Map<RegistryKey<Item>, Item> ALL = new HashMap<>();

    private static final net.minecraft.item.ToolMaterial SCARAB = ScarabGemMaterial.SCARAB_GEM;

    // Core items
    public static final Item BIO_FOSSIL = register("bio_fossil",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.bio_fossil"), new Item.Settings());
    public static final Item RELIC = register("relic",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.relic"), new Item.Settings());
    public static final Item SCARAB_GEM = register("scarab_gem",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.scarab_gem"), new Item.Settings());
    public static final Item DINOPEDIA = register("dinopedia", ItemDinopedia::new, new Item.Settings());
    public static final Item FERN_SEED = register("fern_seed",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.fern_seed"), new Item.Settings());

    // Ancient artifacts — sword uses applySwordSettings for proper damage/speed
    public static final Item ANCIENT_SWORD = register("ancient_sword", Item::new,
            SCARAB.applySwordSettings(new Item.Settings(), 3.0f, -2.4f));
    public static final Item ANCIENT_HELMET = register("ancient_helmet");
    public static final Item BROKEN_SWORD = register("broken_sword",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.broken_artifact"), new Item.Settings());
    public static final Item BROKEN_HELMET = register("broken_helmet",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.broken_artifact"), new Item.Settings());

    // Scarab tools
    public static final Item SCARAB_SWORD = register("scarab_sword", Item::new,
            SCARAB.applySwordSettings(new Item.Settings(), 3.0f, -2.4f));
    public static final Item SCARAB_AXE = register("scarab_axe",
            settings -> new AxeItem(SCARAB, 5.0f, -3.0f, settings), new Item.Settings());
    public static final Item SCARAB_PICKAXE = register("scarab_pickaxe", Item::new,
            SCARAB.applyToolSettings(new Item.Settings(), BlockTags.PICKAXE_MINEABLE, 1.0f, -2.8f, 0.0f));
    public static final Item SCARAB_SHOVEL = register("scarab_shovel",
            settings -> new ShovelItem(SCARAB, 1.5f, -3.0f, settings), new Item.Settings());
    public static final Item SCARAB_HOE = register("scarab_hoe",
            settings -> new HoeItem(SCARAB, -3.0f, 0.0f, settings), new Item.Settings());

    public static void registerDinosaurItems() {
        for (Dinosaur d : DinosaurRegistry.all().values()) {
            // DNA
            register(d.id + "_dna", settings -> new TooltipItem(settings, "tooltip.fossilsarch2.dna"),
                    new Item.Settings());

            // Egg
            register(d.id + "_egg", DinoEggItem::new, new Item.Settings().maxCount(1));

            // Raw meat
            register(d.id + "_meat", Item::new, new Item.Settings()
                    .food(new FoodComponent.Builder()
                            .nutrition(d.meat_nutrition)
                            .saturationModifier(d.meat_saturation)
                            .build()));

            // Cooked meat
            register(d.id + "_cooked_meat", Item::new, new Item.Settings()
                    .food(new FoodComponent.Builder()
                            .nutrition(d.cooked_meat_nutrition)
                            .saturationModifier(d.cooked_meat_saturation)
                            .build()));
        }
    }

    private static Item register(String path) {
        return register(path, null, null);
    }

    private static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(FossilsArch2Mod.MOD_ID, path));

        if (factory == null) {
            factory = Item::new;
        }

        if (settings == null) {
            settings = new Item.Settings();
        }

        Item item = Items.register(key, factory, settings);

        ALL.put(key, item);

        return item;
    }
}
