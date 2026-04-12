package mod.fossilsarch2.world;

import mod.fossilsarch2.FossilsArch2Mod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModWorldGeneration {

    private static final ResourceKey<PlacedFeature> FOSSIL_ORE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "fossil_ore"));

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                FOSSIL_ORE_PLACED);
    }
}
