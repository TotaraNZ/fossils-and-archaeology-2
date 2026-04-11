package mod.fossilsarch2;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.registry.DinosaurRegistry;
import mod.fossilsarch2.registry.ModBlocks;
import mod.fossilsarch2.registry.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.Models;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

public class FossilsArch2DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        pack.addProvider(ItemModelProvider::new);
        pack.addProvider(DinoRecipeProvider::new);
    }

    // --- Item Models ---

    private static class ItemModelProvider extends FabricModelProvider {
        public ItemModelProvider(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generateItemModels(ItemModelGenerator generator) {
            for (Item item : ModItems.ALL.values()) {
                generator.register(item, Models.GENERATED);
            }
        }

        @Override
        public void generateBlockStateModels(BlockStateModelGenerator generator) {
        }
    }

    // --- Recipes ---

    private static class DinoRecipeProvider extends FabricRecipeProvider {
        public DinoRecipeProvider(FabricDataOutput output,
                CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup registries, RecipeExporter exporter) {
            return new RecipeGenerator(registries, exporter) {
                @Override
                public void generate() {
                    // === Machine blocks ===

                    // Analyser: iron + relic + bio-fossil
                    createShaped(RecipeCategory.DECORATIONS, ModBlocks.ANALYSER)
                            .pattern("IRI")
                            .pattern("IBI")
                            .input('I', Items.IRON_INGOT)
                            .input('R', ModItems.RELIC)
                            .input('B', ModItems.BIO_FOSSIL)
                            .criterion("has_bio_fossil", conditionsFromItem(ModItems.BIO_FOSSIL))
                            .offerTo(exporter);

                    // Cultivator: glass + green dye + water bucket + iron
                    createShaped(RecipeCategory.DECORATIONS, ModBlocks.CULTIVATOR)
                            .pattern("GDG")
                            .pattern("GWG")
                            .pattern("III")
                            .input('G', Items.GLASS)
                            .input('D', Items.GREEN_DYE)
                            .input('W', Items.WATER_BUCKET)
                            .input('I', Items.IRON_INGOT)
                            .criterion("has_bio_fossil", conditionsFromItem(ModItems.BIO_FOSSIL))
                            .offerTo(exporter);

                    // Worktable: paper over crafting table
                    createShaped(RecipeCategory.DECORATIONS, ModBlocks.WORKTABLE)
                            .pattern("P")
                            .pattern("C")
                            .input('P', Items.PAPER)
                            .input('C', Items.CRAFTING_TABLE)
                            .criterion("has_relic", conditionsFromItem(ModItems.RELIC))
                            .offerTo(exporter);

                    // Feeder: iron + glass + stone button + bucket + stone
                    createShaped(RecipeCategory.DECORATIONS, ModBlocks.FEEDER)
                            .pattern("IGI")
                            .pattern("TBS")
                            .pattern("SSS")
                            .input('I', Items.IRON_INGOT)
                            .input('G', Items.GLASS)
                            .input('T', Items.STONE_BUTTON)
                            .input('B', Items.BUCKET)
                            .input('S', Items.STONE)
                            .criterion("has_bio_fossil", conditionsFromItem(ModItems.BIO_FOSSIL))
                            .offerTo(exporter);

                    // === Scarab tools (gold + gem or diamond + gem) ===

                    createShapeless(RecipeCategory.COMBAT, ModItems.SCARAB_SWORD)
                            .input(Items.GOLDEN_SWORD)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_sword_from_gold"));

                    createShapeless(RecipeCategory.COMBAT, ModItems.SCARAB_SWORD)
                            .input(Items.DIAMOND_SWORD)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_sword_from_diamond"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_AXE)
                            .input(Items.GOLDEN_AXE)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_axe_from_gold"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_AXE)
                            .input(Items.DIAMOND_AXE)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_axe_from_diamond"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_PICKAXE)
                            .input(Items.GOLDEN_PICKAXE)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_pickaxe_from_gold"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_PICKAXE)
                            .input(Items.DIAMOND_PICKAXE)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_pickaxe_from_diamond"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_SHOVEL)
                            .input(Items.GOLDEN_SHOVEL)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_shovel_from_gold"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_SHOVEL)
                            .input(Items.DIAMOND_SHOVEL)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_shovel_from_diamond"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_HOE)
                            .input(Items.GOLDEN_HOE)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_hoe_from_gold"));

                    createShapeless(RecipeCategory.TOOLS, ModItems.SCARAB_HOE)
                            .input(Items.DIAMOND_HOE)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_scarab_gem", conditionsFromItem(ModItems.SCARAB_GEM))
                            .offerTo(exporter, recipeKey("scarab_hoe_from_diamond"));

                    // === Ancient artifacts (repair with scarab gem) ===

                    createShapeless(RecipeCategory.COMBAT, ModItems.ANCIENT_SWORD)
                            .input(ModItems.BROKEN_SWORD)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_broken_sword", conditionsFromItem(ModItems.BROKEN_SWORD))
                            .offerTo(exporter);

                    createShapeless(RecipeCategory.COMBAT, ModItems.ANCIENT_HELMET)
                            .input(ModItems.BROKEN_HELMET)
                            .input(ModItems.SCARAB_GEM)
                            .criterion("has_broken_helmet", conditionsFromItem(ModItems.BROKEN_HELMET))
                            .offerTo(exporter);

                    // === Dinopedia ===

                    createShapeless(RecipeCategory.MISC, ModItems.DINOPEDIA)
                            .input(Items.BOOK)
                            .input(ModItems.BIO_FOSSIL)
                            .criterion("has_bio_fossil", conditionsFromItem(ModItems.BIO_FOSSIL))
                            .offerTo(exporter);

                    // === Dinosaur meat cooking (smelting + smoker + campfire) ===

                    for (Dinosaur d : DinosaurRegistry.all().values()) {
                        Item rawMeat = Registries.ITEM.get(
                                Identifier.of(FossilsArch2Mod.MOD_ID, d.id + "_meat"));
                        Item cookedMeat = Registries.ITEM.get(
                                Identifier.of(FossilsArch2Mod.MOD_ID, d.id + "_cooked_meat"));

                        if (rawMeat == Items.AIR || cookedMeat == Items.AIR) continue;

                        offerSmelting(List.of(rawMeat), RecipeCategory.FOOD, cookedMeat,
                                0.35f, 200, d.id);

                        offerFoodCookingRecipe("smoking",
                                net.minecraft.recipe.RecipeSerializer.SMOKING,
                                net.minecraft.recipe.SmokingRecipe::new,
                                100, rawMeat, cookedMeat, 0.35f);

                        offerFoodCookingRecipe("campfire_cooking",
                                net.minecraft.recipe.RecipeSerializer.CAMPFIRE_COOKING,
                                net.minecraft.recipe.CampfireCookingRecipe::new,
                                600, rawMeat, cookedMeat, 0.35f);
                    }
                }

                private net.minecraft.registry.RegistryKey<net.minecraft.recipe.Recipe<?>> recipeKey(String path) {
                    return net.minecraft.registry.RegistryKey.of(
                            net.minecraft.registry.RegistryKeys.RECIPE,
                            Identifier.of(FossilsArch2Mod.MOD_ID, path));
                }
            };
        }

        @Override
        public String getName() {
            return "Fossils & Archaeology 2 Recipes";
        }
    }
}
