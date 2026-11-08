package mod.fossilsarch2.item;


import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemDinopedia extends Item {

	public ItemDinopedia(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
		if (entity instanceof DinosaurEntity && !user.level().isClientSide()) {
			ModNetworking.sendDinopedia((ServerPlayer) user, entity.getId());
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
