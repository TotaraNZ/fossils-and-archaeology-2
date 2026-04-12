package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.CultivatorBlockEntity;
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

public class CultivatorScreenHandler extends AbstractContainerMenu {

    private final Container inventory;
    private final ContainerData propertyDelegate;

    // Client constructor
    public CultivatorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(CultivatorBlockEntity.INVENTORY_SIZE),
                new SimpleContainerData(4));
    }

    // Server constructor
    public CultivatorScreenHandler(int syncId, Inventory playerInventory,
                                    Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.CULTIVATOR, syncId);
        checkContainerSize(inventory, CultivatorBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.startOpen(playerInventory.player);

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

        this.addDataSlots(propertyDelegate);
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
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();

            int cultivatorSlots = CultivatorBlockEntity.INVENTORY_SIZE;
            int totalSlots = cultivatorSlots + 36;

            if (slotIndex < cultivatorSlots) {
                // From cultivator to player
                if (!this.moveItemStackTo(original, cultivatorSlots, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to input slot first, then fuel
                if (!this.moveItemStackTo(original, 0, 1, false)) {
                    if (!this.moveItemStackTo(original, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (original.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
}
