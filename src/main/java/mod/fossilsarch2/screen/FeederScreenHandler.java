package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.registry.ModScreenHandlers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class FeederScreenHandler extends AbstractContainerMenu {

    private final Container inventory;
    private final ContainerData propertyDelegate;

    public FeederScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(FeederBlockEntity.INVENTORY_SIZE), new SimpleContainerData(3));
    }

    public FeederScreenHandler(int syncId, Inventory playerInventory,
                                Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.FEEDER, syncId);
        checkContainerSize(inventory, FeederBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);

        // Meat slot — matches Revival ContainerFeeder (60, 62)
        this.addSlot(new Slot(inventory, FeederBlockEntity.MEAT_SLOT, 60, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FeederBlockEntity.acceptsItem(FeederBlockEntity.MEAT_SLOT, stack);
            }
        });
        // Veg slot — matches Revival ContainerFeeder (104, 62)
        this.addSlot(new Slot(inventory, FeederBlockEntity.VEG_SLOT, 104, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FeederBlockEntity.acceptsItem(FeederBlockEntity.VEG_SLOT, stack);
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        this.addDataSlots(propertyDelegate);
    }

    public int getMeatLevel() { return propertyDelegate.get(0); }
    public int getVegLevel() { return propertyDelegate.get(1); }
    public int getMaxFood() { int m = propertyDelegate.get(2); return m > 0 ? m : 1; }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();
            if (slotIndex < 2) {
                if (!this.moveItemStackTo(original, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                if (FeederBlockEntity.acceptsItem(FeederBlockEntity.MEAT_SLOT, original)) {
                    if (!this.moveItemStackTo(original, FeederBlockEntity.MEAT_SLOT, FeederBlockEntity.MEAT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (FeederBlockEntity.acceptsItem(FeederBlockEntity.VEG_SLOT, original)) {
                    if (!this.moveItemStackTo(original, FeederBlockEntity.VEG_SLOT, FeederBlockEntity.VEG_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex < 29) {
                    if (!this.moveItemStackTo(original, 29, 38, false)) return ItemStack.EMPTY;
                } else if (slotIndex < 38) {
                    if (!this.moveItemStackTo(original, 2, 29, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) { return this.inventory.stillValid(player); }
}
