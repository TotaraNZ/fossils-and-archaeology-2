package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.CultivatorBlockEntity;
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

public class CultivatorScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    // Client constructor
    public CultivatorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CultivatorBlockEntity.INVENTORY_SIZE),
                new ArrayPropertyDelegate(4));
    }

    // Server constructor
    public CultivatorScreenHandler(int syncId, PlayerInventory playerInventory,
                                    Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.CULTIVATOR, syncId);
        checkSize(inventory, CultivatorBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);

        // DNA input slot (49, 20)
        this.addSlot(new Slot(inventory, CultivatorBlockEntity.INPUT_SLOT, 49, 20));
        // Fuel slot (81, 54)
        this.addSlot(new Slot(inventory, CultivatorBlockEntity.FUEL_SLOT, 81, 54));
        // Output slot (116, 21)
        this.addSlot(new Slot(inventory, CultivatorBlockEntity.OUTPUT_SLOT, 116, 21));

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addProperties(propertyDelegate);
    }

    public int getCookProgress() {
        return propertyDelegate.get(0);
    }

    public int getMaxCookTime() {
        int max = propertyDelegate.get(1);
        return max > 0 ? max : 1;
    }

    public float getCookProgressRatio() {
        return (float) getCookProgress() / (float) getMaxCookTime();
    }

    public int getBurnTime() {
        return propertyDelegate.get(2);
    }

    public int getCurrentItemBurnTime() {
        int t = propertyDelegate.get(3);
        return t > 0 ? t : 1;
    }

    public float getBurnTimeRatio() {
        return (float) getBurnTime() / (float) getCurrentItemBurnTime();
    }

    public boolean isBurning() {
        return getBurnTime() > 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();

            int cultivatorSlots = CultivatorBlockEntity.INVENTORY_SIZE;
            int totalSlots = cultivatorSlots + 36;

            if (slotIndex < cultivatorSlots) {
                // From cultivator to player
                if (!this.insertItem(original, cultivatorSlots, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to input slot first, then fuel
                if (!this.insertItem(original, 0, 1, false)) {
                    if (!this.insertItem(original, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (original.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
