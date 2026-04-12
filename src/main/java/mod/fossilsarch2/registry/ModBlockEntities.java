package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.entity.AnalyserBlockEntity;
import mod.fossilsarch2.block.entity.CultivatorBlockEntity;
import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.block.entity.SuspiciousStoneBlockEntity;
import mod.fossilsarch2.block.entity.WorktableBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final BlockEntityType<AnalyserBlockEntity> ANALYSER = register(
            "analyser",
            FabricBlockEntityTypeBuilder.create(AnalyserBlockEntity::new, ModBlocks.ANALYSER).build());

    public static final BlockEntityType<CultivatorBlockEntity> CULTIVATOR = register(
            "cultivator",
            FabricBlockEntityTypeBuilder.create(CultivatorBlockEntity::new, ModBlocks.CULTIVATOR).build());

    public static final BlockEntityType<FeederBlockEntity> FEEDER = register(
            "feeder",
            FabricBlockEntityTypeBuilder.create(FeederBlockEntity::new, ModBlocks.FEEDER).build());

    public static final BlockEntityType<WorktableBlockEntity> WORKTABLE = register(
            "worktable",
            FabricBlockEntityTypeBuilder.create(WorktableBlockEntity::new, ModBlocks.WORKTABLE).build());

    public static final BlockEntityType<SuspiciousStoneBlockEntity> SUSPICIOUS_STONE = register(
            "suspicious_stone",
            FabricBlockEntityTypeBuilder.create(SuspiciousStoneBlockEntity::new, ModBlocks.SUSPICIOUS_STONE).build());

    private static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path),
                blockEntityType);
    }

    public static void init() {
    }
}
