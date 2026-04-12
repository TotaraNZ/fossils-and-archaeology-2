package mod.fossilsarch2.block;

import com.mojang.serialization.MapCodec;

import mod.fossilsarch2.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Prehistoric fern with 4 growth stages (0-3).
 * - Not yet mature: bonemeal advances growth, random ticks advance growth
 * - Mature (age 3): bonemeal spreads seedlings nearby, random ticks slowly spread
 */
public class FernBlock extends BushBlock implements BonemealableBlock {

    public static final MapCodec<FernBlock> CODEC = BlockBehaviour.simpleCodec(FernBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private static final VoxelShape[] SHAPES = {
            Block.box(4, 0, 4, 12, 5, 12),   // stage 0: seedling
            Block.box(3, 0, 3, 13, 8, 13),   // stage 1: small
            Block.box(2, 0, 2, 14, 12, 14),  // stage 2: medium
            Block.box(1, 0, 1, 15, 16, 15),  // stage 3: full
    };

    public FernBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<BushBlock> codec() { return (MapCodec<BushBlock>) (MapCodec<?>) CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.GRASS_BLOCK) || floor.is(Blocks.DIRT) || floor.is(Blocks.PODZOL)
                || floor.is(Blocks.MOSS_BLOCK) || floor.is(Blocks.MUD);
    }

    // --- Random tick: grow if young, spread if mature ---

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            // Growing: 20% chance per tick
            if (random.nextInt(5) == 0) {
                world.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
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
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            world.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        } else {
            for (int i = 0; i < 2 + random.nextInt(3); i++) {
                trySpread(world, pos, random);
            }
        }
    }

    private void trySpread(ServerLevel world, BlockPos origin, RandomSource random) {
        BlockPos target = origin.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
        if (world.isEmptyBlock(target) && mayPlaceOn(world.getBlockState(target.below()), world, target.below())) {
            world.setBlock(target, ModBlocks.FERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
