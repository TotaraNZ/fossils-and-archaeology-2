package mod.fossilsarch2.world;

import java.util.List;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Keys and datagen bootstraps for this mod's worldgen. The feature, placed feature and biome
 * modifier are all generated to JSON under {@code src/generated/server} by the datagen pipeline.
 */
public final class ModWorldGeneration {

    public static final ResourceKey<Feature> FOSSIL_ORE = ResourceKey.create(
            Registries.FEATURE, Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "fossil_ore"));

    public static final ResourceKey<PlacedFeature> FOSSIL_ORE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "fossil_ore"));

    public static final ResourceKey<BiomeModifier> ADD_FOSSIL_ORE = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "add_fossil_ore"));

    private static final int VEIN_SIZE = 8;
    private static final int VEINS_PER_CHUNK = 10;

    public static void features(BootstrapContext<Feature> ctx) {
        ctx.register(FOSSIL_ORE, new OreFeature(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                ModBlocks.SUSPICIOUS_STONE.get().defaultBlockState(), VEIN_SIZE));
    }

    public static void placedFeatures(BootstrapContext<PlacedFeature> ctx) {
        ctx.register(FOSSIL_ORE_PLACED, new PlacedFeature(
                ctx.lookup(Registries.FEATURE).getOrThrow(FOSSIL_ORE),
                List.of(CountPlacement.of(VEINS_PER_CHUNK),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(60)),
                        BiomeFilter.biome())));
    }

    private ModWorldGeneration() {}
}
