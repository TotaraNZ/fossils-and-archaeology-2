package mod.fossilsarch2.registry;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.AnalyserBlock;
import mod.fossilsarch2.block.CultivatorBlock;
import mod.fossilsarch2.block.FeederBlock;
import mod.fossilsarch2.block.FernBlock;
import mod.fossilsarch2.block.SuspiciousStoneBlock;
import mod.fossilsarch2.block.WorktableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FossilsArch2Mod.MOD_ID);
	private static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(FossilsArch2Mod.MOD_ID);

	public static final DeferredBlock<SuspiciousStoneBlock> SUSPICIOUS_STONE = BLOCKS.registerBlock(
			"suspicious_stone",
			SuspiciousStoneBlock::new,
			props -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
					.sound(SoundType.STONE)
					.pushReaction(PushReaction.DESTROY));

	public static final DeferredBlock<AnalyserBlock> ANALYSER = BLOCKS.registerBlock(
			"analyser",
			AnalyserBlock::new,
			props -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

	public static final DeferredBlock<CultivatorBlock> CULTIVATOR = BLOCKS.registerBlock(
			"cultivator",
			CultivatorBlock::new,
			props -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state ->
					state.getValue(CultivatorBlock.LIT) ? 13 : 0));

	public static final DeferredBlock<FeederBlock> FEEDER = BLOCKS.registerBlock(
			"feeder",
			FeederBlock::new,
			props -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

	public static final DeferredBlock<FernBlock> FERN = BLOCKS.registerBlock(
			"fern",
			FernBlock::new,
			props -> BlockBehaviour.Properties.ofFullCopy(Blocks.FERN).randomTicks());

	public static final DeferredBlock<WorktableBlock> WORKTABLE = BLOCKS.registerBlock(
			"worktable",
			WorktableBlock::new,
			props -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state ->
					state.getValue(WorktableBlock.LIT) ? 13 : 0));

	// Paired BlockItems use the registerSimpleBlockItem helper.
	public static final DeferredItem<BlockItem> SUSPICIOUS_STONE_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(SUSPICIOUS_STONE);
	public static final DeferredItem<BlockItem> ANALYSER_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(ANALYSER);
	public static final DeferredItem<BlockItem> CULTIVATOR_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(CULTIVATOR);
	public static final DeferredItem<BlockItem> FEEDER_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(FEEDER);
	public static final DeferredItem<BlockItem> FERN_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(FERN);
	public static final DeferredItem<BlockItem> WORKTABLE_ITEM = BLOCK_ITEMS.registerSimpleBlockItem(WORKTABLE);

	public static void register(IEventBus modEventBus) {
		BLOCKS.register(modEventBus);
		BLOCK_ITEMS.register(modEventBus);
	}

	private ModBlocks() {}
}
