package mod.fossilsarch2.block.entity;

import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.registry.ModItems;
import mod.fossilsarch2.screen.WorktableScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

/**
 * Worktable repairs broken ancient artifacts using relics as fuel.
 * Same fuel-based pattern as the cultivator.
 */
public class WorktableBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Inventory {

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

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int cookTime = 0;
    private int burnTime = 0;
    private int currentItemBurnTime = 0;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() { return 4; }
    };

    public WorktableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WORKTABLE, pos, state);
    }

    // --- Inventory ---
    @Override public int size() { return INVENTORY_SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override public ItemStack removeStack(int slot) { return Inventories.removeStack(items, slot); }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public void clear() { items.clear(); }

    // --- NBT ---
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, items, registries);
        nbt.putInt("CookTime", cookTime);
        nbt.putInt("BurnTime", burnTime);
        nbt.putInt("CurrentItemBurnTime", currentItemBurnTime);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, items, registries);
        cookTime = nbt.getInt("CookTime", 0);
        burnTime = nbt.getInt("BurnTime", 0);
        currentItemBurnTime = nbt.getInt("CurrentItemBurnTime", 0);
    }

    // --- Screen ---
    @Override
    public Text getDisplayName() { return Text.translatable("block.fossilsarch2.worktable"); }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new WorktableScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---
    public static void tick(World world, BlockPos pos, BlockState state, WorktableBlockEntity entity) {
        if (world.isClient()) return;

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
                    entity.items.get(FUEL_SLOT).decrement(1);
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
        if (wasBurning != isBurning && state.contains(mod.fossilsarch2.block.WorktableBlock.LIT)) {
            world.setBlockState(pos, state.with(mod.fossilsarch2.block.WorktableBlock.LIT, isBurning), Block.NOTIFY_ALL);
        }

        if (dirty) entity.markDirty();
    }

    private boolean canSmelt() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) return false;
        Item result = RECIPES.get(input.getItem());
        if (result == null) return false;
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return output.isOf(result) && output.getCount() < output.getMaxCount();
    }

    private void smeltItem() {
        if (!canSmelt()) return;
        ItemStack input = items.get(INPUT_SLOT);
        Item result = RECIPES.get(input.getItem());
        if (result == null) return;

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, new ItemStack(result, 1));
        } else if (output.isOf(result)) {
            output.increment(1);
        }
        input.decrement(1);
    }

    private static int getFuelTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        // Only relics can fuel the worktable
        if (stack.isOf(ModItems.RELIC)) return 300;
        return 0;
    }
}
