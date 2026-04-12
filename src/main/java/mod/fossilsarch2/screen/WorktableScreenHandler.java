package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.WorktableBlockEntity;
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

public class WorktableScreenHandler extends AbstractContainerMenu {

    private final Container inventory;
    private final ContainerData propertyDelegate;

    public WorktableScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(WorktableBlockEntity.INVENTORY_SIZE), new SimpleContainerData(4));
    }

    public WorktableScreenHandler(int syncId, Inventory playerInventory,
                                   Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.WORKTABLE, syncId);
        checkContainerSize(inventory, WorktableBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        inventory.startOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, WorktableBlockEntity.INPUT_SLOT, 49, 20));
        this.addSlot(new Slot(inventory, WorktableBlockEntity.FUEL_SLOT, 81, 54));
        this.addSlot(new Slot(inventory, WorktableBlockEntity.OUTPUT_SLOT, 116, 21));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        this.addDataSlots(propertyDelegate);
    }

    public float getCookProgressRatio() {
        int max = propertyDelegate.get(1);
        return max > 0 ? (float) propertyDelegate.get(0) / max : 0;
    }

    public boolean isBurning() { return propertyDelegate.get(2) > 0; }

    public float getBurnTimeRatio() {
        int max = propertyDelegate.get(3);
        return max > 0 ? (float) propertyDelegate.get(2) / max : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();
            if (slotIndex < 3) {
                if (!this.moveItemStackTo(original, 3, 39, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(original, 0, 1, false))
                    if (!this.moveItemStackTo(original, 1, 2, false)) return ItemStack.EMPTY;
            }
            if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) { return this.inventory.stillValid(player); }
}
