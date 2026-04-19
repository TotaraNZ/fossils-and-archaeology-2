package mod.fossilsarch2.item;

import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.registry.ModEntities;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class DinoEggItem extends Item {

    public DinoEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        // Derive species from item registry name: "fossilsarch2:{species}_egg" -> "{species}"
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = itemId.getPath();
        if (!path.endsWith("_egg")) return InteractionResult.PASS;
        String species = path.substring(0, path.length() - 4);

        DinoEggEntity egg = new DinoEggEntity(ModEntities.DINO_EGG.get(), level);
        egg.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        egg.setDinoId(species);
        if (player != null) {
            egg.setOwnerUuid(player.getUUID());
        }

        ((ServerLevel) level).addFreshEntity(egg);
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent,
            Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Component.translatable("tooltip.fossilsarch2.egg").withStyle(ChatFormatting.GRAY));
    }
}
