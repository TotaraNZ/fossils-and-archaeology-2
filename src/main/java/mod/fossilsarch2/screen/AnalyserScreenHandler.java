package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.AnalyserBlockEntity;
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

public class AnalyserScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    // Client constructor
    public AnalyserScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(AnalyserBlockEntity.INVENTORY_SIZE),
                new ArrayPropertyDelegate(2));
    }

    // Server constructor
    public AnalyserScreenHandler(int syncId, PlayerInventory playerInventory,
            Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.ANALYSER, syncId);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        checkSize(inventory, AnalyserBlockEntity.INVENTORY_SIZE);
        inventory.onOpen(playerInventory.player);

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

        addProperties(propertyDelegate);
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
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            int analyserSlots = AnalyserBlockEntity.INVENTORY_SIZE;

            if (slotIndex < analyserSlots) {
                if (!insertItem(originalStack, analyserSlots, analyserSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!insertItem(originalStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }
}
