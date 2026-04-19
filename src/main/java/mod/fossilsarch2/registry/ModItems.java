package mod.fossilsarch2.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.item.DinoEggItem;
import mod.fossilsarch2.item.ItemDinopedia;
import mod.fossilsarch2.item.RemainderFoodItem;
import mod.fossilsarch2.item.ScarabGemMaterial;
import mod.fossilsarch2.item.TooltipItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FossilsArch2Mod.MOD_ID);

	private static final ToolMaterial SCARAB = ScarabGemMaterial.SCARAB_GEM;

	private static final Map<String, DeferredItem<? extends Item>> DINOSAUR_ITEMS = new HashMap<>();
	private static final Map<String, Dinosaur> DINOSAURS = new LinkedHashMap<>();

	// Core items
	public static final DeferredItem<TooltipItem> BIO_FOSSIL = ITEMS.registerItem("bio_fossil",
			props -> new TooltipItem(props, "tooltip.fossilsarch2.bio_fossil"));
	public static final DeferredItem<TooltipItem> RELIC = ITEMS.registerItem("relic",
			props -> new TooltipItem(props, "tooltip.fossilsarch2.relic"));
	public static final DeferredItem<TooltipItem> SCARAB_GEM = ITEMS.registerItem("scarab_gem",
			props -> new TooltipItem(props, "tooltip.fossilsarch2.scarab_gem"));
	public static final DeferredItem<ItemDinopedia> DINOPEDIA = ITEMS.registerItem("dinopedia", ItemDinopedia::new);
	public static final DeferredItem<TooltipItem> FERN_SEED = ITEMS.registerItem("fern_seed",
			props -> new TooltipItem(props, "tooltip.fossilsarch2.fern_seed"));
	public static final DeferredItem<RemainderFoodItem> CHICKEN_SOUP_RAW = ITEMS.registerItem("chicken_soup_raw",
			RemainderFoodItem::new,
			props -> props.food(new FoodProperties.Builder().nutrition(4).saturationModifier(2.0f).build()));
	public static final DeferredItem<RemainderFoodItem> CHICKEN_SOUP_COOKED = ITEMS.registerItem("chicken_soup_cooked",
			RemainderFoodItem::new,
			props -> props.food(new FoodProperties.Builder().nutrition(8).saturationModifier(2.0f).build()));
	public static final DeferredItem<TooltipItem> ESSENCE_CHICKEN = ITEMS.registerItem("essence_chicken",
			props -> new TooltipItem(props, "tooltip.fossilsarch2.essence_chicken"));

	// Ancient artifacts
	public static final DeferredItem<Item> ANCIENT_SWORD = ITEMS.registerItem("ancient_sword",
			Item::new, props -> props.sword(SCARAB, 3.0f, -2.4f));
	public static final DeferredItem<Item> ANCIENT_HELMET = ITEMS.registerSimpleItem("ancient_helmet");
	public static final DeferredItem<TooltipItem> BROKEN_SWORD = ITEMS.registerItem("broken_sword",
			props -> new TooltipItem(props, "tooltip.fossilsarch2.broken_artifact"));
	public static final DeferredItem<TooltipItem> BROKEN_HELMET = ITEMS.registerItem("broken_helmet",
			props -> new TooltipItem(props, "tooltip.fossilsarch2.broken_artifact"));

	// Scarab tools (sword + pickaxe via vanilla property delegates;
	// axe/shovel/hoe still need subclass instances since their delegates don't exist as Item.Properties methods)
	public static final DeferredItem<Item> SCARAB_SWORD = ITEMS.registerItem("scarab_sword",
			Item::new, props -> props.sword(SCARAB, 3.0f, -2.4f));
	public static final DeferredItem<AxeItem> SCARAB_AXE = ITEMS.registerItem("scarab_axe",
			props -> new AxeItem(SCARAB, 5.0f, -3.0f, props));
	public static final DeferredItem<Item> SCARAB_PICKAXE = ITEMS.registerItem("scarab_pickaxe",
			Item::new, props -> props.pickaxe(SCARAB, 1.0f, -2.8f));
	public static final DeferredItem<ShovelItem> SCARAB_SHOVEL = ITEMS.registerItem("scarab_shovel",
			props -> new ShovelItem(SCARAB, 1.5f, -3.0f, props));
	public static final DeferredItem<HoeItem> SCARAB_HOE = ITEMS.registerItem("scarab_hoe",
			props -> new HoeItem(SCARAB, -3.0f, 0.0f, props));

	public static void register(IEventBus modEventBus, Map<String, Dinosaur> dinosaurs) {
		DINOSAURS.clear();
		DINOSAURS.putAll(dinosaurs);
		for (Map.Entry<String, Dinosaur> entry : dinosaurs.entrySet()) {
			String id = entry.getKey();
			Dinosaur d = entry.getValue();
			DINOSAUR_ITEMS.put(id + "_dna", ITEMS.registerItem(id + "_dna",
					props -> new TooltipItem(props, "tooltip.fossilsarch2.dna")));
			DINOSAUR_ITEMS.put(id + "_egg", ITEMS.registerItem(id + "_egg",
					DinoEggItem::new, props -> props.stacksTo(1)));
			DINOSAUR_ITEMS.put(id + "_meat", ITEMS.registerItem(id + "_meat",
					Item::new, props -> props.food(new FoodProperties.Builder()
							.nutrition(d.food().meatNutrition())
							.saturationModifier(d.food().meatSaturation())
							.build())));
			DINOSAUR_ITEMS.put(id + "_cooked_meat", ITEMS.registerItem(id + "_cooked_meat",
					Item::new, props -> props.food(new FoodProperties.Builder()
							.nutrition(d.food().cookedMeatNutrition())
							.saturationModifier(d.food().cookedMeatSaturation())
							.build())));
		}

		ITEMS.register(modEventBus);
	}

	public static Map<String, Dinosaur> dinosaurs() {
		return Collections.unmodifiableMap(DINOSAURS);
	}

	public static DeferredItem<? extends Item> dinosaurItem(String key) {
		return DINOSAUR_ITEMS.get(key);
	}

	private ModItems() {}
}
