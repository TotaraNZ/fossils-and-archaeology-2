package mod.fossilsarch2.world;

import mod.fossilsarch2.FossilsArch2Mod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public final class ModWorldGeneration {

    private static final RegistryKey<PlacedFeature> FOSSIL_ORE_PLACED = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE, Identifier.of(FossilsArch2Mod.MOD_ID, "fossil_ore"));

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                FOSSIL_ORE_PLACED);
    }
}
