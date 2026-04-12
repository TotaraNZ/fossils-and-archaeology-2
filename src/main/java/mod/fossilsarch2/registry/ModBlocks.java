package mod.fossilsarch2.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.AnalyserBlock;
import mod.fossilsarch2.block.CultivatorBlock;
import mod.fossilsarch2.block.FeederBlock;
import mod.fossilsarch2.block.FernBlock;
import mod.fossilsarch2.block.SuspiciousStoneBlock;
import mod.fossilsarch2.block.WorktableBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {

        public static Map<ResourceKey<Block>, Block> ALL = new HashMap<>();

        public static final Block SUSPICIOUS_STONE = register(
                        "suspicious_stone",
                        SuspiciousStoneBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                                .sound(SoundType.STONE)
                                .pushReaction(PushReaction.DESTROY));

        public static final Block ANALYSER = register(
                        "analyser",
                        AnalyserBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());

        public static final Block CULTIVATOR = register(
                        "cultivator",
                        CultivatorBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state ->
                                state.getValue(CultivatorBlock.LIT) ? 13 : 0));

        public static final Block FEEDER = register(
                        "feeder",
                        FeederBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

        public static final Block FERN = register(
                        "fern",
                        FernBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.FERN).randomTicks());

        public static final Block WORKTABLE = register(
                        "worktable",
                        WorktableBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state ->
                                state.getValue(WorktableBlock.LIT) ? 13 : 0));

        private static Block register(
                        String path,
                        Function<BlockBehaviour.Properties, Block> factory,
                        BlockBehaviour.Properties settings) {
                ResourceKey<Block> registryKey = ResourceKey.create(Registries.BLOCK,
                                Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path));

                settings.setId(registryKey);
                Block block = factory.apply(settings);
                Registry.register(BuiltInRegistries.BLOCK, registryKey, block);

                // Register block item
                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                                registryKey.identifier());
                Item.Properties itemProps = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
                BlockItem blockItem = new BlockItem(block, itemProps);
                Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

                ALL.put(registryKey, block);
                return block;
        }

        public static void init() {
        }
}
