package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.AnalyserBlock;
import mod.fossilsarch2.block.CultivatorBlock;
import mod.fossilsarch2.block.FeederBlock;
import mod.fossilsarch2.block.FernBlock;
import mod.fossilsarch2.block.WorktableBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModBlocks {

        public static Map<RegistryKey<Block>, Block> ALL = new HashMap<>();

        public static final Block FOSSIL = register(
                        "fossil",
                        Block::new,
                        AbstractBlock.Settings.copy(Blocks.STONE));

        public static final Block ANALYSER = register(
                        "analyser",
                        AnalyserBlock::new,
                        AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());

        public static final Block CULTIVATOR = register(
                        "cultivator",
                        CultivatorBlock::new,
                        AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).luminance(state ->
                                state.get(CultivatorBlock.LIT) ? 13 : 0));

        public static final Block FEEDER = register(
                        "feeder",
                        FeederBlock::new,
                        AbstractBlock.Settings.copy(Blocks.OAK_PLANKS));

        public static final Block FERN = register(
                        "fern",
                        FernBlock::new,
                        AbstractBlock.Settings.copy(Blocks.FERN).ticksRandomly());

        public static final Block WORKTABLE = register(
                        "worktable",
                        WorktableBlock::new,
                        AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).luminance(state ->
                                state.get(WorktableBlock.LIT) ? 13 : 0));

        private static Block register(
                        String path,
                        Function<AbstractBlock.Settings, Block> factory,
                        AbstractBlock.Settings settings) {
                RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK,
                                Identifier.of(FossilsArch2Mod.MOD_ID, path));

                Block block = Blocks.register(registryKey, factory, settings);
                Items.register(block);

                ALL.put(registryKey, block);
                return block;
        }

        public static void init() {
        }
}
