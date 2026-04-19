package mod.fossilsarch2.data;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.registry.ModAdvancements;
import mod.fossilsarch2.registry.ModBlocks;
import mod.fossilsarch2.registry.ModItemTags;
import mod.fossilsarch2.registry.ModItems;
import mod.fossilsarch2.world.ModWorldGeneration;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Datagen entry point. Wired through {@code @EventBusSubscriber} on the mod
 * event bus when the {@code data} run argument is set.
 */
@EventBusSubscriber(modid = FossilsArch2Mod.MOD_ID)
public final class FossilsArch2DataGenerator {

	private FossilsArch2DataGenerator() {}

	@SubscribeEvent
	public static void onGatherServer(GatherDataEvent.Server event) {
		// Datapack registry entries (biome modifiers) — fires before the providers below,
		// so they see the modded entries when generating dependent JSON.
		event.createDatapackRegistryObjects(datapackEntries(), Set.of(FossilsArch2Mod.MOD_ID));

		event.createProvider(FossilsRecipeRunner::new);
		event.createProvider((output, registries) -> new LootTableProvider(output, Set.of(),
				List.of(new LootTableProvider.SubProviderEntry(BlockLootProvider::new, LootContextParamSets.BLOCK)),
				registries));
		event.createProvider(FossilsItemTagProvider::new);
		event.createProvider((output, registries) -> new AdvancementProvider(output, registries,
				List.of(new FossilsAdvancementSubProvider())));
	}

	@SubscribeEvent
	public static void onGatherClient(GatherDataEvent.Client event) {
		event.createProvider(FossilsModelProvider::new);
	}

	private static RegistrySetBuilder datapackEntries() {
		return new RegistrySetBuilder()
				.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
					HolderSet<Biome> overworld = ctx.lookup(Registries.BIOME).getOrThrow(Tags.Biomes.IS_OVERWORLD);
					Holder<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE)
							.getOrThrow(ModWorldGeneration.FOSSIL_ORE_PLACED);
					ctx.register(ModWorldGeneration.ADD_FOSSIL_ORE,
							new AddFeaturesBiomeModifier(overworld, HolderSet.direct(placed),
									GenerationStep.Decoration.UNDERGROUND_ORES));
				});
	}

	// --- Models ---

	private static class FossilsModelProvider extends ModelProvider {
		FossilsModelProvider(PackOutput output) {
			super(output, FossilsArch2Mod.MOD_ID);
		}

		@Override
		protected void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
			BuiltInRegistries.ITEM.forEach(item -> {
				Identifier id = BuiltInRegistries.ITEM.getKey(item);
				if (!id.getNamespace().equals(FossilsArch2Mod.MOD_ID)) return;
				// BlockItems use the block's name for their model; vanilla generates them automatically
				// when the block model is generated. Since we hand-write blockstates/models, register
				// flat fallbacks only for non-block items.
				if (item instanceof net.minecraft.world.item.BlockItem) return;
				items.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
			});
		}

		@Override
		protected java.util.stream.Stream<? extends net.minecraft.core.Holder<net.minecraft.world.level.block.Block>> getKnownBlocks() {
			// Blockstate definitions are hand-written under src/main/resources/assets — skip validation.
			return java.util.stream.Stream.empty();
		}
	}

	// --- Recipes ---

	private static class FossilsRecipeRunner extends RecipeProvider.Runner {
		FossilsRecipeRunner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
			return new FossilsRecipes(registries, output);
		}

		@Override
		public String getName() {
			return "Fossils & Archaeology 2 Recipes";
		}
	}

	private static class FossilsRecipes extends RecipeProvider {
		protected FossilsRecipes(HolderLookup.Provider registries, RecipeOutput output) {
			super(registries, output);
		}

		@Override
		protected void buildRecipes() {
			shaped(RecipeCategory.DECORATIONS, ModBlocks.ANALYSER.get())
					.pattern("IRI").pattern("IBI")
					.define('I', Items.IRON_INGOT)
					.define('R', ModItems.RELIC.get())
					.define('B', ModItems.BIO_FOSSIL.get())
					.unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL.get()))
					.save(output);

			shaped(RecipeCategory.DECORATIONS, ModBlocks.CULTIVATOR.get())
					.pattern("GDG").pattern("GWG").pattern("III")
					.define('G', Items.GLASS).define('D', Items.GREEN_DYE)
					.define('W', Items.WATER_BUCKET).define('I', Items.IRON_INGOT)
					.unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL.get()))
					.save(output);

			shaped(RecipeCategory.DECORATIONS, ModBlocks.WORKTABLE.get())
					.pattern("P").pattern("C")
					.define('P', Items.PAPER).define('C', Items.CRAFTING_TABLE)
					.unlockedBy("has_relic", has(ModItems.RELIC.get()))
					.save(output);

			shaped(RecipeCategory.DECORATIONS, ModBlocks.FEEDER.get())
					.pattern("IGI").pattern("TBS").pattern("SSS")
					.define('I', Items.IRON_INGOT).define('G', Items.GLASS)
					.define('T', Items.STONE_BUTTON).define('B', Items.BUCKET)
					.define('S', Items.STONE)
					.unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL.get()))
					.save(output);

			scarabUpgrade(RecipeCategory.COMBAT, ModItems.SCARAB_SWORD.get(), Items.GOLDEN_SWORD, "scarab_sword_from_gold");
			scarabUpgrade(RecipeCategory.COMBAT, ModItems.SCARAB_SWORD.get(), Items.DIAMOND_SWORD, "scarab_sword_from_diamond");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_AXE.get(), Items.GOLDEN_AXE, "scarab_axe_from_gold");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_AXE.get(), Items.DIAMOND_AXE, "scarab_axe_from_diamond");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_PICKAXE.get(), Items.GOLDEN_PICKAXE, "scarab_pickaxe_from_gold");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_PICKAXE.get(), Items.DIAMOND_PICKAXE, "scarab_pickaxe_from_diamond");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_SHOVEL.get(), Items.GOLDEN_SHOVEL, "scarab_shovel_from_gold");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_SHOVEL.get(), Items.DIAMOND_SHOVEL, "scarab_shovel_from_diamond");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_HOE.get(), Items.GOLDEN_HOE, "scarab_hoe_from_gold");
			scarabUpgrade(RecipeCategory.TOOLS, ModItems.SCARAB_HOE.get(), Items.DIAMOND_HOE, "scarab_hoe_from_diamond");

			shapeless(RecipeCategory.COMBAT, ModItems.ANCIENT_SWORD.get())
					.requires(ModItems.BROKEN_SWORD.get()).requires(ModItems.SCARAB_GEM.get())
					.unlockedBy("has_broken_sword", has(ModItems.BROKEN_SWORD.get()))
					.save(output);

			shapeless(RecipeCategory.COMBAT, ModItems.ANCIENT_HELMET.get())
					.requires(ModItems.BROKEN_HELMET.get()).requires(ModItems.SCARAB_GEM.get())
					.unlockedBy("has_broken_helmet", has(ModItems.BROKEN_HELMET.get()))
					.save(output);

			shapeless(RecipeCategory.MISC, ModItems.DINOPEDIA.get())
					.requires(Items.BOOK).requires(ModItems.BIO_FOSSIL.get())
					.unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL.get()))
					.save(output);

			shapeless(RecipeCategory.FOOD, ModItems.CHICKEN_SOUP_RAW.get())
					.requires(Items.BUCKET).requires(Items.CHICKEN)
					.unlockedBy("has_chicken", has(Items.CHICKEN))
					.save(output);

			SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.CHICKEN_SOUP_RAW.get()), RecipeCategory.FOOD,
							CookingBookCategory.FOOD, ModItems.CHICKEN_SOUP_COOKED.get(), 1.0f, 200)
					.unlockedBy("has_chicken_soup_raw", has(ModItems.CHICKEN_SOUP_RAW.get()))
					.save(output, recipeKey("chicken_soup_cooked_from_smelting"));

			SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItems.CHICKEN_SOUP_RAW.get()), RecipeCategory.FOOD,
							ModItems.CHICKEN_SOUP_COOKED.get(), 1.0f, 100)
					.unlockedBy("has_chicken_soup_raw", has(ModItems.CHICKEN_SOUP_RAW.get()))
					.save(output, recipeKey("chicken_soup_cooked_from_smoking"));

			shaped(RecipeCategory.MISC, ModItems.ESSENCE_CHICKEN.get(), 8)
					.pattern("GGG").pattern("GCG").pattern("GGG")
					.define('G', Items.GLASS_BOTTLE).define('C', ModItems.CHICKEN_SOUP_COOKED.get())
					.unlockedBy("has_chicken_soup_cooked", has(ModItems.CHICKEN_SOUP_COOKED.get()))
					.save(output);

			for (String id : ModItems.dinosaurs().keySet()) {
				Item rawMeat = lookup(id + "_meat");
				Item cookedMeat = lookup(id + "_cooked_meat");
				if (rawMeat == Items.AIR || cookedMeat == Items.AIR) continue;

				SimpleCookingRecipeBuilder.smelting(Ingredient.of(rawMeat), RecipeCategory.FOOD,
								CookingBookCategory.FOOD, cookedMeat, 0.35f, 200)
						.unlockedBy("has_" + id + "_meat", has(rawMeat))
						.save(output, recipeKey(id + "_cooked_meat_from_smelting"));

				SimpleCookingRecipeBuilder.smoking(Ingredient.of(rawMeat), RecipeCategory.FOOD,
								cookedMeat, 0.35f, 100)
						.unlockedBy("has_" + id + "_meat", has(rawMeat))
						.save(output, recipeKey(id + "_cooked_meat_from_smoking"));

				SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(rawMeat), RecipeCategory.FOOD,
								cookedMeat, 0.35f, 600)
						.unlockedBy("has_" + id + "_meat", has(rawMeat))
						.save(output, recipeKey(id + "_cooked_meat_from_campfire_cooking"));
			}
		}

		private void scarabUpgrade(RecipeCategory category, ItemLike result, ItemLike base, String name) {
			shapeless(category, result)
					.requires(base)
					.requires(ModItems.SCARAB_GEM.get())
					.unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM.get()))
					.save(output, recipeKey(name));
		}

		private static Item lookup(String path) {
			return BuiltInRegistries.ITEM.getValue(
					Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path));
		}

		private static ResourceKey<Recipe<?>> recipeKey(String path) {
			return ResourceKey.create(Registries.RECIPE,
					Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path));
		}
	}

	// --- Block loot ---

	private static class BlockLootProvider extends BlockLootSubProvider {
		protected BlockLootProvider(HolderLookup.Provider registries) {
			super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
		}

		@Override
		public void generate() {
			dropSelf(ModBlocks.ANALYSER.get());
			dropSelf(ModBlocks.CULTIVATOR.get());
			dropSelf(ModBlocks.FEEDER.get());
			dropSelf(ModBlocks.WORKTABLE.get());
		}

		@Override
		protected Iterable<Block> getKnownBlocks() {
			return List.of(
					ModBlocks.ANALYSER.get(),
					ModBlocks.CULTIVATOR.get(),
					ModBlocks.FEEDER.get(),
					ModBlocks.WORKTABLE.get());
		}
	}

	// --- Item tags ---

	private static class FossilsItemTagProvider extends ItemTagsProvider {
		FossilsItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries, FossilsArch2Mod.MOD_ID);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			for (String id : ModItems.dinosaurs().keySet()) {
				addItemIfPresent(ModItemTags.DINO_MEAT, id + "_meat");
				addItemIfPresent(ModItemTags.DINO_MEAT, id + "_cooked_meat");
			}

			tag(ModItemTags.FEEDER_MEAT)
					.addOptionalTag(ItemTags.MEAT)
					.addOptionalTag(ItemTags.FISHES)
					.addTag(ModItemTags.DINO_MEAT);

			tag(ModItemTags.FEEDER_VEGETABLE)
					.addOptionalTag(ItemTags.COW_FOOD)
					.addOptionalTag(ItemTags.HORSE_FOOD)
					.addOptionalTag(ItemTags.PIG_FOOD)
					.addOptionalTag(ItemTags.CHICKEN_FOOD)
					.addOptionalTag(ItemTags.RABBIT_FOOD)
					.addOptionalTag(ItemTags.SHEEP_FOOD)
					.addOptionalTag(ItemTags.LLAMA_FOOD)
					.addOptionalTag(ItemTags.GOAT_FOOD)
					.addOptionalTag(ItemTags.TURTLE_FOOD)
					.addOptionalTag(ItemTags.SNIFFER_FOOD)
					.addOptionalTag(ItemTags.CAMEL_FOOD)
					.addOptionalTag(ItemTags.PANDA_FOOD)
					.addOptionalTag(ItemTags.STRIDER_FOOD)
					.addOptionalTag(ItemTags.ZOMBIE_HORSE_FOOD)
					.add(Items.APPLE)
					.add(Items.MELON_SLICE)
					.add(Items.SWEET_BERRIES)
					.add(Items.GLOW_BERRIES)
					.add(Items.KELP)
					.add(Items.DRIED_KELP)
					.add(Items.BREAD)
					.add(Items.COOKIE)
					.add(Items.PUMPKIN_PIE)
					.add(ModItems.FERN_SEED.get());
		}

		private void addItemIfPresent(net.minecraft.tags.TagKey<Item> tagKey, String path) {
			Item item = BuiltInRegistries.ITEM.getValue(
					Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path));
			if (item != Items.AIR) {
				tag(tagKey).add(item);
			}
		}
	}

	// --- Advancements ---

	private static class FossilsAdvancementSubProvider implements AdvancementSubProvider {
		private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("minecraft",
				"textures/gui/advancements/backgrounds/stone.png");

		@Override
		public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> consumer) {
			AdvancementHolder root = Advancement.Builder.advancement()
					.display(ModBlocks.SUSPICIOUS_STONE.get(),
							Component.translatable("advancement.fossilsarch2.root.title"),
							Component.translatable("advancement.fossilsarch2.root.description"),
							BACKGROUND, AdvancementType.TASK, false, false, false)
					.addCriterion("entered_world",
							PlayerTrigger.TriggerInstance.located(Optional.empty()))
					.save(consumer, ModAdvancements.ROOT.toString());

			AdvancementHolder discoverBioFossil = Advancement.Builder.advancement()
					.parent(root)
					.display(ModItems.BIO_FOSSIL.get(),
							Component.translatable("advancement.fossilsarch2.discover_bio_fossil.title"),
							Component.translatable("advancement.fossilsarch2.discover_bio_fossil.description"),
							null, AdvancementType.TASK, true, true, false)
					.addCriterion("has_bio_fossil",
							InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BIO_FOSSIL.get()))
					.save(consumer, ModAdvancements.DISCOVER_BIO_FOSSIL.toString());

			AdvancementHolder extractDna = Advancement.Builder.advancement()
					.parent(discoverBioFossil)
					.display(ModItems.DINOPEDIA.get(),
							Component.translatable("advancement.fossilsarch2.extract_dna.title"),
							Component.translatable("advancement.fossilsarch2.extract_dna.description"),
							null, AdvancementType.TASK, true, true, false)
					.addCriterion("has_dna",
							InventoryChangeTrigger.TriggerInstance.hasItems(getDnaItems()))
					.save(consumer, ModAdvancements.EXTRACT_DNA.toString());

			AdvancementHolder cultivateEgg = Advancement.Builder.advancement()
					.parent(extractDna)
					.display(Items.EGG,
							Component.translatable("advancement.fossilsarch2.cultivate_egg.title"),
							Component.translatable("advancement.fossilsarch2.cultivate_egg.description"),
							null, AdvancementType.TASK, true, true, false)
					.addCriterion("has_egg",
							InventoryChangeTrigger.TriggerInstance.hasItems(getEggItems()))
					.save(consumer, ModAdvancements.CULTIVATE_EGG.toString());

			Advancement.Builder.advancement()
					.parent(cultivateEgg)
					.display(ModItems.DINOPEDIA.get(),
							Component.translatable("advancement.fossilsarch2.hatch_dinosaur.title"),
							Component.translatable("advancement.fossilsarch2.hatch_dinosaur.description"),
							null, AdvancementType.GOAL, true, true, false)
					.addCriterion(ModAdvancements.HATCHED_DINOSAUR_CRITERION,
							CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
					.save(consumer, ModAdvancements.HATCH_DINOSAUR.toString());
		}

		private static ItemLike[] getDnaItems() {
			return ModItems.dinosaurs().keySet().stream()
					.map(id -> BuiltInRegistries.ITEM.getValue(
							Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, id + "_dna")))
					.filter(item -> item != Items.AIR)
					.toArray(ItemLike[]::new);
		}

		private static ItemLike[] getEggItems() {
			return ModItems.dinosaurs().keySet().stream()
					.map(id -> BuiltInRegistries.ITEM.getValue(
							Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, id + "_egg")))
					.filter(item -> item != Items.AIR)
					.toArray(ItemLike[]::new);
		}
	}
}
