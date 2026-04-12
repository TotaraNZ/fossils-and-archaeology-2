package mod.fossilsarch2;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.registry.DinosaurRegistry;
import mod.fossilsarch2.registry.ModAdvancements;
import mod.fossilsarch2.registry.ModBlocks;
import mod.fossilsarch2.registry.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

public class FossilsArch2DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        pack.addProvider(ItemModelProvider::new);
        pack.addProvider(DinoRecipeProvider::new);
        pack.addProvider(FossilsAdvancementProvider::new);
    }

    // --- Item Models ---

    private static class ItemModelProvider extends FabricModelProvider {
        public ItemModelProvider(FabricPackOutput output) {
            super(output);
        }

        @Override
        public void generateItemModels(ItemModelGenerators generator) {
            for (Item item : ModItems.ALL.values()) {
                generator.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
            }
        }

        @Override
        public void generateBlockStateModels(BlockModelGenerators generator) {
        }
    }

    // --- Recipes ---

    private static class DinoRecipeProvider extends FabricRecipeProvider {
        public DinoRecipeProvider(FabricPackOutput output,
                CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new RecipeProvider(registries, output) {
                @Override
                public void buildRecipes() {
                    shaped(RecipeCategory.DECORATIONS, ModBlocks.ANALYSER)
                            .pattern("IRI")
                            .pattern("IBI")
                            .define('I', Items.IRON_INGOT)
                            .define('R', ModItems.RELIC)
                            .define('B', ModItems.BIO_FOSSIL)
                            .unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL))
                            .save(output);

                    shaped(RecipeCategory.DECORATIONS, ModBlocks.CULTIVATOR)
                            .pattern("GDG")
                            .pattern("GWG")
                            .pattern("III")
                            .define('G', Items.GLASS)
                            .define('D', Items.GREEN_DYE)
                            .define('W', Items.WATER_BUCKET)
                            .define('I', Items.IRON_INGOT)
                            .unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL))
                            .save(output);

                    shaped(RecipeCategory.DECORATIONS, ModBlocks.WORKTABLE)
                            .pattern("P")
                            .pattern("C")
                            .define('P', Items.PAPER)
                            .define('C', Items.CRAFTING_TABLE)
                            .unlockedBy("has_relic", has(ModItems.RELIC))
                            .save(output);

                    shaped(RecipeCategory.DECORATIONS, ModBlocks.FEEDER)
                            .pattern("IGI")
                            .pattern("TBS")
                            .pattern("SSS")
                            .define('I', Items.IRON_INGOT)
                            .define('G', Items.GLASS)
                            .define('T', Items.STONE_BUTTON)
                            .define('B', Items.BUCKET)
                            .define('S', Items.STONE)
                            .unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL))
                            .save(output);

                    shapeless(RecipeCategory.COMBAT, ModItems.SCARAB_SWORD)
                            .requires(Items.GOLDEN_SWORD)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_sword_from_gold"));

                    shapeless(RecipeCategory.COMBAT, ModItems.SCARAB_SWORD)
                            .requires(Items.DIAMOND_SWORD)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_sword_from_diamond"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_AXE)
                            .requires(Items.GOLDEN_AXE)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_axe_from_gold"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_AXE)
                            .requires(Items.DIAMOND_AXE)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_axe_from_diamond"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_PICKAXE)
                            .requires(Items.GOLDEN_PICKAXE)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_pickaxe_from_gold"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_PICKAXE)
                            .requires(Items.DIAMOND_PICKAXE)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_pickaxe_from_diamond"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_SHOVEL)
                            .requires(Items.GOLDEN_SHOVEL)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_shovel_from_gold"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_SHOVEL)
                            .requires(Items.DIAMOND_SHOVEL)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_shovel_from_diamond"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_HOE)
                            .requires(Items.GOLDEN_HOE)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_hoe_from_gold"));

                    shapeless(RecipeCategory.TOOLS, ModItems.SCARAB_HOE)
                            .requires(Items.DIAMOND_HOE)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_scarab_gem", has(ModItems.SCARAB_GEM))
                            .save(output, recipeKey("scarab_hoe_from_diamond"));

                    shapeless(RecipeCategory.COMBAT, ModItems.ANCIENT_SWORD)
                            .requires(ModItems.BROKEN_SWORD)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_broken_sword", has(ModItems.BROKEN_SWORD))
                            .save(output);

                    shapeless(RecipeCategory.COMBAT, ModItems.ANCIENT_HELMET)
                            .requires(ModItems.BROKEN_HELMET)
                            .requires(ModItems.SCARAB_GEM)
                            .unlockedBy("has_broken_helmet", has(ModItems.BROKEN_HELMET))
                            .save(output);

                    shapeless(RecipeCategory.MISC, ModItems.DINOPEDIA)
                            .requires(Items.BOOK)
                            .requires(ModItems.BIO_FOSSIL)
                            .unlockedBy("has_bio_fossil", has(ModItems.BIO_FOSSIL))
                            .save(output);

                    shapeless(RecipeCategory.FOOD, ModItems.CHICKEN_SOUP_RAW)
                            .requires(Items.BUCKET)
                            .requires(Items.CHICKEN)
                            .unlockedBy("has_chicken", has(Items.CHICKEN))
                            .save(output);

                    SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.CHICKEN_SOUP_RAW), RecipeCategory.FOOD,
                                    CookingBookCategory.FOOD, ModItems.CHICKEN_SOUP_COOKED, 1.0f, 200)
                            .unlockedBy("has_chicken_soup_raw", has(ModItems.CHICKEN_SOUP_RAW))
                            .save(output, recipeKey("chicken_soup_cooked_from_smelting"));

                    SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItems.CHICKEN_SOUP_RAW), RecipeCategory.FOOD,
                                    ModItems.CHICKEN_SOUP_COOKED, 1.0f, 100)
                            .unlockedBy("has_chicken_soup_raw", has(ModItems.CHICKEN_SOUP_RAW))
                            .save(output, recipeKey("chicken_soup_cooked_from_smoking"));

                    shaped(RecipeCategory.MISC, ModItems.ESSENCE_CHICKEN, 8)
                            .pattern("GGG")
                            .pattern("GCG")
                            .pattern("GGG")
                            .define('G', Items.GLASS_BOTTLE)
                            .define('C', ModItems.CHICKEN_SOUP_COOKED)
                            .unlockedBy("has_chicken_soup_cooked", has(ModItems.CHICKEN_SOUP_COOKED))
                            .save(output);

                    for (Dinosaur d : DinosaurRegistry.all().values()) {
                        Item rawMeat = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, d.id + "_meat"));
                        Item cookedMeat = BuiltInRegistries.ITEM.getValue(
                                Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, d.id + "_cooked_meat"));

                        if (rawMeat == Items.AIR || cookedMeat == Items.AIR) continue;

                        SimpleCookingRecipeBuilder.smelting(Ingredient.of(rawMeat), RecipeCategory.FOOD,
                                        CookingBookCategory.FOOD, cookedMeat, 0.35f, 200)
                                .unlockedBy("has_" + d.id + "_meat", has(rawMeat))
                                .save(output, recipeKey(d.id + "_cooked_meat_from_smelting"));

                        SimpleCookingRecipeBuilder.smoking(Ingredient.of(rawMeat), RecipeCategory.FOOD,
                                        cookedMeat, 0.35f, 100)
                                .unlockedBy("has_" + d.id + "_meat", has(rawMeat))
                                .save(output, recipeKey(d.id + "_cooked_meat_from_smoking"));

                        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(rawMeat), RecipeCategory.FOOD,
                                        cookedMeat, 0.35f, 600)
                                .unlockedBy("has_" + d.id + "_meat", has(rawMeat))
                                .save(output, recipeKey(d.id + "_cooked_meat_from_campfire_cooking"));
                    }
                }

                private ResourceKey<Recipe<?>> recipeKey(String path) {
                    return ResourceKey.create(
                            Registries.RECIPE,
                            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path));
                }
            };
        }

        @Override
        public String getName() {
            return "Fossils & Archaeology 2 Recipes";
        }
    }

    // --- Advancements ---

    private static class FossilsAdvancementProvider extends FabricAdvancementProvider {
        private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("minecraft",
                "textures/gui/advancements/backgrounds/stone.png");

        protected FossilsAdvancementProvider(FabricPackOutput output,
                CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        public void generateAdvancement(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
            AdvancementHolder root = Advancement.Builder.advancement()
                    .display(ModBlocks.SUSPICIOUS_STONE,
                            Component.translatable("advancement.fossilsarch2.root.title"),
                            Component.translatable("advancement.fossilsarch2.root.description"),
                            BACKGROUND,
                            AdvancementType.TASK,
                            false,
                            false,
                            false)
                    .addCriterion("entered_world", PlayerTrigger.TriggerInstance.located(
                            java.util.Optional.empty()))
                    .save(consumer, ModAdvancements.ROOT.toString());

            AdvancementHolder discoverBioFossil = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModItems.BIO_FOSSIL,
                            Component.translatable("advancement.fossilsarch2.discover_bio_fossil.title"),
                            Component.translatable("advancement.fossilsarch2.discover_bio_fossil.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false)
                    .addCriterion("has_bio_fossil", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BIO_FOSSIL))
                    .save(consumer, ModAdvancements.DISCOVER_BIO_FOSSIL.toString());

            AdvancementHolder extractDna = Advancement.Builder.advancement()
                    .parent(discoverBioFossil)
                    .display(ModItems.DINOPEDIA,
                            Component.translatable("advancement.fossilsarch2.extract_dna.title"),
                            Component.translatable("advancement.fossilsarch2.extract_dna.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false)
                    .addCriterion("has_dna", InventoryChangeTrigger.TriggerInstance.hasItems(getDnaItems()))
                    .save(consumer, ModAdvancements.EXTRACT_DNA.toString());

            AdvancementHolder cultivateEgg = Advancement.Builder.advancement()
                    .parent(extractDna)
                    .display(Items.EGG,
                            Component.translatable("advancement.fossilsarch2.cultivate_egg.title"),
                            Component.translatable("advancement.fossilsarch2.cultivate_egg.description"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false)
                    .addCriterion("has_egg", InventoryChangeTrigger.TriggerInstance.hasItems(getEggItems()))
                    .save(consumer, ModAdvancements.CULTIVATE_EGG.toString());

            Advancement.Builder.advancement()
                    .parent(cultivateEgg)
                    .display(ModItems.DINOPEDIA,
                            Component.translatable("advancement.fossilsarch2.hatch_dinosaur.title"),
                            Component.translatable("advancement.fossilsarch2.hatch_dinosaur.description"),
                            null,
                            AdvancementType.GOAL,
                            true,
                            true,
                            false)
                    .addCriterion(ModAdvancements.HATCHED_DINOSAUR_CRITERION,
                            CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(consumer, ModAdvancements.HATCH_DINOSAUR.toString());
        }

        private static ItemLike[] getDnaItems() {
            return DinosaurRegistry.all().values().stream()
                    .map(dino -> BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, dino.id + "_dna")))
                    .filter(item -> item != Items.AIR)
                    .toArray(ItemLike[]::new);
        }

        private static ItemLike[] getEggItems() {
            return DinosaurRegistry.all().values().stream()
                    .map(dino -> BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, dino.id + "_egg")))
                    .filter(item -> item != Items.AIR)
                    .toArray(ItemLike[]::new);
        }
    }
}
