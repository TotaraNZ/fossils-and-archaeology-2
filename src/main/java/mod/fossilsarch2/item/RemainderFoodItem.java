package mod.fossilsarch2.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class RemainderFoodItem extends Item {
    public RemainderFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        ItemStack remainder = new ItemStack(Items.BUCKET);

        if (result.isEmpty()) {
            return remainder;
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }

        return result;
    }
}
