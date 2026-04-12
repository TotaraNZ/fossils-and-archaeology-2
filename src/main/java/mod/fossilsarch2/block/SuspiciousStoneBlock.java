package mod.fossilsarch2.block;

import com.mojang.serialization.MapCodec;

import mod.fossilsarch2.block.entity.SuspiciousStoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class SuspiciousStoneBlock extends BaseEntityBlock {

    public static final MapCodec<SuspiciousStoneBlock> CODEC = BlockBehaviour.simpleCodec(SuspiciousStoneBlock::new);
    public static final IntegerProperty DUSTED = BlockStateProperties.DUSTED;

    private final Block baseBlock = Blocks.STONE;
    private final SoundEvent brushingSound = SoundEvents.BRUSH_GRAVEL;
    private final SoundEvent brushingCompleteSound = SoundEvents.BRUSH_GRAVEL_COMPLETED;

    public SuspiciousStoneBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(DUSTED, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DUSTED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuspiciousStoneBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        world.scheduleTick(pos, this, 2);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            LevelReader world,
            ScheduledTickAccess tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        tickView.scheduleTick(pos, this, 2);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SuspiciousStoneBlockEntity suspiciousStoneBlockEntity) {
            suspiciousStoneBlockEntity.scheduledTick(world);
        }
    }

    public Block getBaseBlock() {
        return baseBlock;
    }

    public SoundEvent getBrushingSound() {
        return brushingSound;
    }

    public SoundEvent getBrushingCompleteSound() {
        return brushingCompleteSound;
    }
}
