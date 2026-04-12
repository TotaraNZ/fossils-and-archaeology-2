package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.item.DinoEggItem;
import mod.fossilsarch2.item.ItemDinopedia;
import mod.fossilsarch2.item.RemainderFoodItem;
import mod.fossilsarch2.item.ScarabGemMaterial;
import mod.fossilsarch2.item.TooltipItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;

public final class ModItems {

    public static final Map<ResourceKey<Item>, Item> ALL = new HashMap<>();

    private static final ToolMaterial SCARAB = ScarabGemMaterial.SCARAB_GEM;

    // Core items
    public static final Item BIO_FOSSIL = register("bio_fossil",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.bio_fossil"), new Item.Properties());
    public static final Item RELIC = register("relic",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.relic"), new Item.Properties());
    public static final Item SCARAB_GEM = register("scarab_gem",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.scarab_gem"), new Item.Properties());
    public static final Item DINOPEDIA = register("dinopedia", ItemDinopedia::new, new Item.Properties());
    public static final Item FERN_SEED = register("fern_seed",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.fern_seed"), new Item.Properties());
    public static final Item CHICKEN_SOUP_RAW = register("chicken_soup_raw",
            RemainderFoodItem::new, new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(2.0f)
                            .build()));
    public static final Item CHICKEN_SOUP_COOKED = register("chicken_soup_cooked",
            RemainderFoodItem::new, new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(2.0f)
                            .build()));
    public static final Item ESSENCE_CHICKEN = register("essence_chicken",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.essence_chicken"), new Item.Properties());

    // Ancient artifacts — sword uses applySwordSettings for proper damage/speed
    public static final Item ANCIENT_SWORD = register("ancient_sword", Item::new,
            SCARAB.applySwordProperties(new Item.Properties(), 3.0f, -2.4f));
    public static final Item ANCIENT_HELMET = register("ancient_helmet");
    public static final Item BROKEN_SWORD = register("broken_sword",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.broken_artifact"), new Item.Properties());
    public static final Item BROKEN_HELMET = register("broken_helmet",
            settings -> new TooltipItem(settings, "tooltip.fossilsarch2.broken_artifact"), new Item.Properties());

    // Scarab tools
    public static final Item SCARAB_SWORD = register("scarab_sword", Item::new,
            SCARAB.applySwordProperties(new Item.Properties(), 3.0f, -2.4f));
    public static final Item SCARAB_AXE = register("scarab_axe",
            settings -> new AxeItem(SCARAB, 5.0f, -3.0f, settings), new Item.Properties());
    public static final Item SCARAB_PICKAXE = register("scarab_pickaxe", Item::new,
            SCARAB.applyToolProperties(new Item.Properties(), BlockTags.MINEABLE_WITH_PICKAXE, 1.0f, -2.8f, 0.0f));
    public static final Item SCARAB_SHOVEL = register("scarab_shovel",
            settings -> new ShovelItem(SCARAB, 1.5f, -3.0f, settings), new Item.Properties());
    public static final Item SCARAB_HOE = register("scarab_hoe",
            settings -> new HoeItem(SCARAB, -3.0f, 0.0f, settings), new Item.Properties());

    public static void registerDinosaurItems() {
        for (Dinosaur d : DinosaurRegistry.all().values()) {
            // DNA
            register(d.id + "_dna", settings -> new TooltipItem(settings, "tooltip.fossilsarch2.dna"),
                    new Item.Properties());

            // Egg
            register(d.id + "_egg", DinoEggItem::new, new Item.Properties().stacksTo(1));

            // Raw meat
            register(d.id + "_meat", Item::new, new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(d.meat_nutrition)
                            .saturationModifier(d.meat_saturation)
                            .build()));

            // Cooked meat
            register(d.id + "_cooked_meat", Item::new, new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(d.cooked_meat_nutrition)
                            .saturationModifier(d.cooked_meat_saturation)
                            .build()));
        }
    }

    private static Item register(String path) {
        return register(path, null, null);
    }

    private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties settings) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path));

        if (factory == null) {
            factory = Item::new;
        }

        if (settings == null) {
            settings = new Item.Properties();
        }

        settings.setId(key);
        Item item = factory.apply(settings);
        Registry.register(BuiltInRegistries.ITEM, key, item);

        ALL.put(key, item);

        return item;
    }
}
