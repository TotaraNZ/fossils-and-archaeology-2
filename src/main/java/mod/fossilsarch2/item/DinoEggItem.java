package mod.fossilsarch2.item;

import mod.fossilsarch2.dinosaur.DinosaurUtils;
import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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

        // Derive species from item registry name: "{ns}:{species}_egg" -> "{ns}:{species}"
        Identifier species = DinosaurUtils.getSpeciesFromItem(stack);
        if (species == null) return InteractionResult.PASS;

        DinoEggEntity egg = new DinoEggEntity(ModEntities.DINO_EGG.get(), level);
        egg.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        egg.setSpeciesId(species);
        if (player != null) {
            egg.setOwnerUuid(player.getUUID());
        }

        ((ServerLevel) level).addFreshEntity(egg);
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
