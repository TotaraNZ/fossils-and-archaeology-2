package mod.fossilsarch2.block;

import com.mojang.serialization.MapCodec;

import mod.fossilsarch2.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * Prehistoric fern with 4 growth stages (0-3).
 * - Not yet mature: bonemeal advances growth, random ticks advance growth
 * - Mature (age 3): bonemeal spreads seedlings nearby, random ticks slowly spread
 */
public class FernBlock extends PlantBlock implements Fertilizable {

    public static final MapCodec<FernBlock> CODEC = Block.createCodec(FernBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntProperty AGE = Properties.AGE_3;

    private static final VoxelShape[] SHAPES = {
            Block.createCuboidShape(4, 0, 4, 12, 5, 12),   // stage 0: seedling
            Block.createCuboidShape(3, 0, 3, 13, 8, 13),   // stage 1: small
            Block.createCuboidShape(2, 0, 2, 14, 12, 14),  // stage 2: medium
            Block.createCuboidShape(1, 0, 1, 15, 16, 15),  // stage 3: full
    };

    public FernBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(AGE, 0));
    }

    @Override
    protected MapCodec<? extends FernBlock> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(AGE)];
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isOf(Blocks.GRASS_BLOCK) || floor.isOf(Blocks.DIRT) || floor.isOf(Blocks.PODZOL)
                || floor.isOf(Blocks.MOSS_BLOCK) || floor.isOf(Blocks.MUD);
    }

    // --- Random tick: grow if young, spread if mature ---

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int age = state.get(AGE);
        if (age < MAX_AGE) {
            // Growing: 20% chance per tick
            if (random.nextInt(5) == 0) {
                world.setBlockState(pos, state.with(AGE, age + 1), Block.NOTIFY_LISTENERS);
            }
        } else {
            // Mature: 10% chance to spread a seedling nearby
            if (random.nextInt(10) == 0) {
                trySpread(world, pos, random);
            }
        }
    }

    // --- Bonemeal: grow if young, spread if mature ---

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        int age = state.get(AGE);
        if (age < MAX_AGE) {
            world.setBlockState(pos, state.with(AGE, age + 1), Block.NOTIFY_LISTENERS);
        } else {
            for (int i = 0; i < 2 + random.nextInt(3); i++) {
                trySpread(world, pos, random);
            }
        }
    }

    private void trySpread(ServerWorld world, BlockPos origin, Random random) {
        BlockPos target = origin.add(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
        if (world.isAir(target) && canPlantOnTop(world.getBlockState(target.down()), world, target.down())) {
            world.setBlockState(target, ModBlocks.FERN.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
    }
}
