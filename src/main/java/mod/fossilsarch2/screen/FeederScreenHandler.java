package mod.fossilsarch2.screen;

import mod.fossilsarch2.block.entity.FeederBlockEntity;
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

public class FeederScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public FeederScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(FeederBlockEntity.INVENTORY_SIZE), new ArrayPropertyDelegate(3));
    }

    public FeederScreenHandler(int syncId, PlayerInventory playerInventory,
                                Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.FEEDER, syncId);
        checkSize(inventory, FeederBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        inventory.onOpen(playerInventory.player);

        // Meat slot — matches Revival ContainerFeeder (60, 62)
        this.addSlot(new Slot(inventory, FeederBlockEntity.MEAT_SLOT, 60, 62));
        // Veg slot — matches Revival ContainerFeeder (104, 62)
        this.addSlot(new Slot(inventory, FeederBlockEntity.VEG_SLOT, 104, 62));

        // Player inventory
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        this.addProperties(propertyDelegate);
    }

    public int getMeatLevel() { return propertyDelegate.get(0); }
    public int getVegLevel() { return propertyDelegate.get(1); }
    public int getMaxFood() { int m = propertyDelegate.get(2); return m > 0 ? m : 1; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();
            if (slotIndex < 2) {
                if (!this.insertItem(original, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(original, 0, 2, false)) return ItemStack.EMPTY;
            }
            if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return this.inventory.canPlayerUse(player); }
}
