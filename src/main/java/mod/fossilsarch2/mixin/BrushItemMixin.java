package mod.fossilsarch2.mixin;

import mod.fossilsarch2.block.SuspiciousStoneBlock;
import mod.fossilsarch2.block.entity.SuspiciousStoneBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushItem.class)
public abstract class BrushItemMixin {

    @Shadow
    private HitResult getHitResult(PlayerEntity player) {
        throw new AssertionError();
    }

    @Shadow
    private void addDustParticles(World world, BlockHitResult hitResult, BlockState state, Vec3d rotationVector, Arm arm) {
        throw new AssertionError();
    }

    @Inject(method = "usageTick", at = @At("HEAD"), cancellable = true)
    private void fossilsarch2$brushSuspiciousStone(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (remainingUseTicks < 0 || !(user instanceof PlayerEntity player)) {
            return;
        }

        HitResult hitResult = getHitResult(player);
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (!(world.getBlockEntity(pos) instanceof SuspiciousStoneBlockEntity suspiciousStoneBlockEntity)) {
            return;
        }

        ci.cancel();

        int useTime = 200 - remainingUseTicks + 1;
        if (useTime % 10 != 5) {
            return;
        }

        BlockState state = world.getBlockState(pos);
        Arm arm = user.getActiveHand() == Hand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();

        if (state.hasBlockBreakParticles() && state.getRenderType() != BlockRenderType.INVISIBLE) {
            addDustParticles(world, blockHitResult, state, user.getRotationVec(0.0f), arm);
        }

        SoundEvent brushingSound = state.getBlock() instanceof SuspiciousStoneBlock suspiciousStoneBlock
                ? suspiciousStoneBlock.getBrushingSound()
                : null;
        if (brushingSound != null) {
            world.playSound(player, pos, brushingSound, SoundCategory.BLOCKS);
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        boolean completed = suspiciousStoneBlockEntity.brush(
                world.getTime(),
                serverWorld,
                user,
                blockHitResult.getSide(),
                stack);
        if (!completed) {
            return;
        }

        EquipmentSlot slot = stack.equals(player.getEquippedStack(EquipmentSlot.OFFHAND))
                ? EquipmentSlot.OFFHAND
                : EquipmentSlot.MAINHAND;
        stack.damage(1, user, slot);
    }
}
