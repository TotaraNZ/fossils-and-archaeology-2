package mod.fossilsarch2.registry;

import java.util.function.Supplier;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.entity.AnalyserBlockEntity;
import mod.fossilsarch2.block.entity.CultivatorBlockEntity;
import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.block.entity.SuspiciousStoneBlockEntity;
import mod.fossilsarch2.block.entity.WorktableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FossilsArch2Mod.MOD_ID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnalyserBlockEntity>> ANALYSER =
			register("analyser", AnalyserBlockEntity::new, ModBlocks.ANALYSER);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CultivatorBlockEntity>> CULTIVATOR =
			register("cultivator", CultivatorBlockEntity::new, ModBlocks.CULTIVATOR);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FeederBlockEntity>> FEEDER =
			register("feeder", FeederBlockEntity::new, ModBlocks.FEEDER);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorktableBlockEntity>> WORKTABLE =
			register("worktable", WorktableBlockEntity::new, ModBlocks.WORKTABLE);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SuspiciousStoneBlockEntity>> SUSPICIOUS_STONE =
			register("suspicious_stone", SuspiciousStoneBlockEntity::new, ModBlocks.SUSPICIOUS_STONE);

	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
			String path,
			BlockEntityType.BlockEntitySupplier<T> factory,
			Supplier<? extends Block> block) {
		return BLOCK_ENTITIES.register(path, () -> new BlockEntityType<>(factory, block.get()));
	}

	public static void register(IEventBus modEventBus) {
		BLOCK_ENTITIES.register(modEventBus);
	}

	private ModBlockEntities() {}
}
