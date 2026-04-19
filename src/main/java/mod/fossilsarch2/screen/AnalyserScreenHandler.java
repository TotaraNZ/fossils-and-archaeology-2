package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.AnalyserBlockEntity;
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

public class AnalyserScreenHandler extends AbstractContainerMenu {

    private final Container inventory;
    private final ContainerData propertyDelegate;

    // Client constructor
    public AnalyserScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(AnalyserBlockEntity.INVENTORY_SIZE),
                new SimpleContainerData(2));
    }

    // Server constructor
    public AnalyserScreenHandler(int syncId, Inventory playerInventory,
            Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.ANALYSER.get(), syncId);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        checkContainerSize(inventory, AnalyserBlockEntity.INVENTORY_SIZE);
        inventory.startOpen(playerInventory.player);

        // 3x3 input grid (slots 0-8) — matches Revival ContainerAnalyzer
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(inventory, row * 3 + col, 20 + col * 18, 17 + row * 18));
            }
        }

        // Primary output slot (slot 9)
        addSlot(new Slot(inventory, 9, 116, 21));

        // Secondary output slots (slots 10-12)
        addSlot(new Slot(inventory, 10, 111, 53));
        addSlot(new Slot(inventory, 11, 129, 53));
        addSlot(new Slot(inventory, 12, 147, 53));

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(propertyDelegate);
    }

    public int getProcessProgress() {
        int processTime = propertyDelegate.get(0);
        int maxProcessTime = propertyDelegate.get(1);
        return maxProcessTime > 0 ? processTime * 21 / maxProcessTime : 0;
    }

    public float getProgressRatio() {
        int processTime = propertyDelegate.get(0);
        int maxProcessTime = propertyDelegate.get(1);
        return maxProcessTime > 0 ? (float) processTime / maxProcessTime : 0;
    }

    public boolean isProcessing() {
        return propertyDelegate.get(0) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            int analyserSlots = AnalyserBlockEntity.INVENTORY_SIZE;

            if (slotIndex < analyserSlots) {
                if (!moveItemStackTo(originalStack, analyserSlots, analyserSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(originalStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }
}
