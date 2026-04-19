package mod.fossilsarch2.block.entity;

import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.registry.ModItemTags;
import mod.fossilsarch2.screen.FeederScreenHandler;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FeederBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int MEAT_SLOT = 0;
    public static final int VEG_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;
    public static final int MAX_FOOD = 1000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int meatLevel = 0;
    private int vegLevel = 0;

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> meatLevel;
                case 1 -> vegLevel;
                case 2 -> MAX_FOOD;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> meatLevel = value;
                case 1 -> vegLevel = value;
            }
        }

        @Override
        public int getCount() { return 3; }
    };

    public FeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEEDER.get(), pos, state);
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
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return acceptsItem(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }

    // --- NBT ---
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("MeatLevel", meatLevel);
        output.putInt("VegLevel", vegLevel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        meatLevel = input.getIntOr("MeatLevel", 0);
        vegLevel = input.getIntOr("VegLevel", 0);
    }

    // --- Screen ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fossilsarch2.feeder");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new FeederScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---
    public static void tick(Level world, BlockPos pos, BlockState state, FeederBlockEntity entity) {
        if (world.isClientSide()) return;

        // Consume items from slots into internal food levels
        ItemStack meatStack = entity.items.get(MEAT_SLOT);
        if (!meatStack.isEmpty() && entity.meatLevel < MAX_FOOD) {
            int foodValue = getFoodValue(MEAT_SLOT, meatStack);
            if (foodValue > 0) {
                entity.meatLevel = Math.min(MAX_FOOD, entity.meatLevel + foodValue);
                meatStack.shrink(1);
                entity.setChanged();
            }
        }

        ItemStack vegStack = entity.items.get(VEG_SLOT);
        if (!vegStack.isEmpty() && entity.vegLevel < MAX_FOOD) {
            int foodValue = getFoodValue(VEG_SLOT, vegStack);
            if (foodValue > 0) {
                entity.vegLevel = Math.min(MAX_FOOD, entity.vegLevel + foodValue);
                vegStack.shrink(1);
                entity.setChanged();
            }
        }
    }

    // --- Feeding API for dinosaurs ---

    public boolean hasMeat() { return meatLevel > 0; }
    public boolean hasVeg() { return vegLevel > 0; }

    public int consumeMeat(int amount) {
        int consumed = Math.min(amount, meatLevel);
        meatLevel -= consumed;
        setChanged();
        return consumed;
    }

    public int consumeVeg(int amount) {
        int consumed = Math.min(amount, vegLevel);
        vegLevel -= consumed;
        setChanged();
        return consumed;
    }

    public int getMeatLevel() { return meatLevel; }
    public int getVegLevel() { return vegLevel; }

    public static boolean acceptsItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;

        return switch (slot) {
            case MEAT_SLOT -> isMeat(stack);
            case VEG_SLOT -> isVegetable(stack);
            default -> false;
        };
    }

    private static boolean isMeat(ItemStack stack) {
        return stack.is(ModItemTags.FEEDER_MEAT);
    }

    private static boolean isVegetable(ItemStack stack) {
        return stack.is(ModItemTags.FEEDER_VEGETABLE);
    }

    private static int getFoodValue(int slot, ItemStack stack) {
        if (!acceptsItem(slot, stack)) return 0;

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            return Math.max(10, food.nutrition() * 10);
        }

        return 25;
    }
}
