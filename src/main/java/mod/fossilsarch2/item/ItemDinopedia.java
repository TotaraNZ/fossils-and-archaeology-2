package mod.fossilsarch2.item;

import java.util.function.Consumer;

import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

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

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent,
			Consumer<Component> textConsumer, TooltipFlag type) {
		super.appendHoverText(stack, context, displayComponent, textConsumer, type);
		textConsumer.accept(Component.translatable("tooltip.fossilsarch2.dinopedia").withStyle(ChatFormatting.GRAY));
	}
}
