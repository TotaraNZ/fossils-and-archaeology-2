package mod.fossilsarch2.block;

import com.mojang.serialization.MapCodec;

import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FeederBlock extends BaseEntityBlock {

    public static final MapCodec<FeederBlock> CODEC = BlockBehaviour.simpleCodec(FeederBlock::new);

    public FeederBlock(Properties settings) {
        super(settings);
    }

    @Override protected MapCodec<? extends FeederBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FeederBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            MenuProvider factory = state.getMenuProvider(world, pos);
            if (factory != null) player.openMenu(factory);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FEEDER.get(), FeederBlockEntity::tick);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof FeederBlockEntity feeder) Containers.dropContents(world, pos, feeder);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }
}
