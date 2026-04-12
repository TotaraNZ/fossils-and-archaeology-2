package mod.fossilsarch2.block;

import com.mojang.serialization.MapCodec;

import mod.fossilsarch2.block.entity.SuspiciousStoneBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class SuspiciousStoneBlock extends BlockWithEntity {

    public static final MapCodec<SuspiciousStoneBlock> CODEC = Block.createCodec(SuspiciousStoneBlock::new);
    public static final IntProperty DUSTED = Properties.DUSTED;

    private final Block baseBlock = Blocks.STONE;
    private final SoundEvent brushingSound = SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL;
    private final SoundEvent brushingCompleteSound = SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL_COMPLETE;

    public SuspiciousStoneBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(DUSTED, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DUSTED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SuspiciousStoneBlockEntity(pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        world.scheduleBlockTick(pos, this, 2);
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random) {
        tickView.scheduleBlockTick(pos, this, 2);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
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
