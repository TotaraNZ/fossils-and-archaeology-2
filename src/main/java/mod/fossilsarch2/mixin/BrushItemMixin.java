package mod.fossilsarch2.mixin;

import mod.fossilsarch2.block.SuspiciousStoneBlock;
import mod.fossilsarch2.block.entity.SuspiciousStoneBlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushItem.class)
public abstract class BrushItemMixin {

    @Shadow
    private HitResult calculateHitResult(Player player) {
        throw new AssertionError();
    }

    @Shadow
    private void spawnDustParticles(Level level, BlockHitResult hitResult, BlockState state, Vec3 rotationVector, HumanoidArm arm) {
        throw new AssertionError();
    }

    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void fossilsarch2$brushSuspiciousStone(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (remainingUseTicks < 0 || !(user instanceof Player player)) {
            return;
        }

        HitResult hitResult = calculateHitResult(player);
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof SuspiciousStoneBlockEntity suspiciousStoneBlockEntity)) {
            return;
        }

        ci.cancel();

        int useTime = 200 - remainingUseTicks + 1;
        if (useTime % 10 != 5) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        HumanoidArm arm = user.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();

        if (state.shouldSpawnTerrainParticles() && state.getRenderShape() != RenderShape.INVISIBLE) {
            spawnDustParticles(level, blockHitResult, state, user.getViewVector(0.0f), arm);
        }

        SoundEvent brushingSound = state.getBlock() instanceof SuspiciousStoneBlock suspiciousStoneBlock
                ? suspiciousStoneBlock.getBrushingSound()
                : null;
        if (brushingSound != null) {
            level.playSound(player, pos, brushingSound, SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean completed = suspiciousStoneBlockEntity.brush(
                level.getGameTime(),
                serverLevel,
                user,
                blockHitResult.getDirection(),
                stack);
        if (!completed) {
            return;
        }

        EquipmentSlot slot = stack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND))
                ? EquipmentSlot.OFFHAND
                : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(1, user, slot);
    }
}
