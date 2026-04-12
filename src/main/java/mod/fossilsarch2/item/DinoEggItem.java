package mod.fossilsarch2.item;

import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.registry.ModEntities;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class DinoEggItem extends Item {

    public DinoEggItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) return ActionResult.SUCCESS;

        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        BlockPos pos = context.getBlockPos().offset(context.getSide());

        // Derive species from item registry name: "fossilsarch2:{species}_egg" → "{species}"
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String path = itemId.getPath();
        if (!path.endsWith("_egg")) return ActionResult.PASS;
        String species = path.substring(0, path.length() - 4);

        DinoEggEntity egg = new DinoEggEntity(ModEntities.DINO_EGG, world);
        egg.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        egg.setDinoId(species);
        if (player != null) {
            egg.setOwnerUuid(player.getUuid());
        }

        world.spawnEntity(egg);
        stack.decrement(1);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
            Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Text.translatable("tooltip.fossilsarch2.egg").formatted(Formatting.GRAY));
    }
}
