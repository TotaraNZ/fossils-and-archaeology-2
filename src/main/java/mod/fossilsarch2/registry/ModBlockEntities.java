package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.entity.AnalyserBlockEntity;
import mod.fossilsarch2.block.entity.CultivatorBlockEntity;
import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.block.entity.WorktableBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

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

    private static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(FossilsArch2Mod.MOD_ID, path),
                blockEntityType);
    }

    public static void init() {
    }
}
