package mod.fossilsarch2.item;

import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.network.DinopediaPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class ItemDinopedia extends Item {

    public ItemDinopedia(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof DinosaurEntity && !user.getWorld().isClient()) {
            ServerPlayNetworking.send((ServerPlayerEntity) user, new DinopediaPayload(entity.getId()));
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
