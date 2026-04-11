package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.WorktableBlockEntity;
import mod.fossilsarch2.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class WorktableScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public WorktableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(WorktableBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(4));
    }

    public WorktableScreenHandler(int syncId, PlayerInventory playerInventory,
                                   Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.WORKTABLE, syncId);
        checkSize(inventory, WorktableBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, WorktableBlockEntity.INPUT_SLOT, 49, 20));
        this.addSlot(new Slot(inventory, WorktableBlockEntity.FUEL_SLOT, 81, 54));
        this.addSlot(new Slot(inventory, WorktableBlockEntity.OUTPUT_SLOT, 116, 21));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        this.addProperties(propertyDelegate);
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
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();
            if (slotIndex < 3) {
                if (!this.insertItem(original, 3, 39, true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(original, 0, 1, false))
                    if (!this.insertItem(original, 1, 2, false)) return ItemStack.EMPTY;
            }
            if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return this.inventory.canPlayerUse(player); }
}
