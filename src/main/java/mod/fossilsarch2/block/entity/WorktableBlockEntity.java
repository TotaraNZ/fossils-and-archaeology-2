package mod.fossilsarch2.block.entity;

import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.registry.ModItems;
import mod.fossilsarch2.screen.WorktableScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Map;

/**
 * Worktable repairs broken ancient artifacts using relics as fuel.
 * Same fuel-based pattern as the cultivator.
 */
public class WorktableBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;
    public static final int MAX_COOK_TIME = 3000; // 150 seconds

    // Repair recipes: input item → output item
    private static final Map<Item, Item> RECIPES = Map.of(
            ModItems.BROKEN_SWORD, ModItems.ANCIENT_SWORD,
            ModItems.BROKEN_HELMET, ModItems.ANCIENT_HELMET
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int cookTime = 0;
    private int burnTime = 0;
    private int currentItemBurnTime = 0;

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> cookTime;
                case 1 -> MAX_COOK_TIME;
                case 2 -> burnTime;
                case 3 -> currentItemBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> cookTime = value;
                case 2 -> burnTime = value;
                case 3 -> currentItemBurnTime = value;
            }
        }

        @Override
        public int getCount() { return 4; }
    };

    public WorktableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORKTABLE, pos, state);
    }

    // --- Inventory ---
    @Override public int getContainerSize() { return INVENTORY_SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }

    // --- NBT ---
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("CookTime", cookTime);
        output.putInt("BurnTime", burnTime);
        output.putInt("CurrentItemBurnTime", currentItemBurnTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        cookTime = input.getIntOr("CookTime", 0);
        burnTime = input.getIntOr("BurnTime", 0);
        currentItemBurnTime = input.getIntOr("CurrentItemBurnTime", 0);
    }

    // --- Screen ---
    @Override
    public Component getDisplayName() { return Component.translatable("block.fossilsarch2.worktable"); }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new WorktableScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---
    public static void tick(Level world, BlockPos pos, BlockState state, WorktableBlockEntity entity) {
        if (world.isClientSide()) return;

        boolean wasBurning = entity.burnTime > 0;
        boolean dirty = false;

        if (entity.burnTime > 0) {
            entity.burnTime--;
            dirty = true;
        }

        if (entity.canSmelt()) {
            if (entity.burnTime == 0) {
                int fuelTime = getFuelTime(entity.items.get(FUEL_SLOT));
                if (fuelTime > 0) {
                    entity.burnTime = fuelTime;
                    entity.currentItemBurnTime = fuelTime;
                    entity.items.get(FUEL_SLOT).shrink(1);
                    dirty = true;
                }
            }

            if (entity.burnTime > 0) {
                entity.cookTime++;
                if (entity.cookTime >= MAX_COOK_TIME) {
                    entity.cookTime = 0;
                    entity.smeltItem();
                }
                dirty = true;
            } else if (entity.cookTime > 0) {
                entity.cookTime = 0;
                dirty = true;
            }
        } else if (entity.cookTime > 0) {
            entity.cookTime = 0;
            dirty = true;
        }

        boolean isBurning = entity.burnTime > 0;
        if (wasBurning != isBurning && state.hasProperty(mod.fossilsarch2.block.WorktableBlock.LIT)) {
            world.setBlock(pos, state.setValue(mod.fossilsarch2.block.WorktableBlock.LIT, isBurning), Block.UPDATE_ALL);
        }

        if (dirty) entity.setChanged();
    }

    private boolean canSmelt() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) return false;
        Item result = RECIPES.get(input.getItem());
        if (result == null) return false;
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return output.is(result) && output.getCount() < output.getMaxStackSize();
    }

    private void smeltItem() {
        if (!canSmelt()) return;
        ItemStack input = items.get(INPUT_SLOT);
        Item result = RECIPES.get(input.getItem());
        if (result == null) return;

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, new ItemStack(result, 1));
        } else if (output.is(result)) {
            output.grow(1);
        }
        input.shrink(1);
    }

    private static int getFuelTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        // Only relics can fuel the worktable
        if (stack.is(ModItems.RELIC)) return 300;
        return 0;
    }
}
