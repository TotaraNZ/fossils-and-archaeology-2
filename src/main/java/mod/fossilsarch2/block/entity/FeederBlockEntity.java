package mod.fossilsarch2.block.entity;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.screen.FeederScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FeederBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Inventory {

    public static final int MEAT_SLOT = 0;
    public static final int VEG_SLOT = 1;
    public static final int INVENTORY_SIZE = 2;
    public static final int MAX_FOOD = 1000;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int meatLevel = 0;
    private int vegLevel = 0;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() { return 3; }
    };

    public FeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEEDER, pos, state);
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
        nbt.putInt("MeatLevel", meatLevel);
        nbt.putInt("VegLevel", vegLevel);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, items, registries);
        meatLevel = nbt.getInt("MeatLevel", 0);
        vegLevel = nbt.getInt("VegLevel", 0);
    }

    // --- Screen ---
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.fossilsarch2.feeder");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new FeederScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---
    public static void tick(World world, BlockPos pos, BlockState state, FeederBlockEntity entity) {
        if (world.isClient()) return;

        // Consume items from slots into internal food levels
        ItemStack meatStack = entity.items.get(MEAT_SLOT);
        if (!meatStack.isEmpty() && entity.meatLevel < MAX_FOOD) {
            if (isMeat(meatStack)) {
                entity.meatLevel = Math.min(MAX_FOOD, entity.meatLevel + 50);
                meatStack.decrement(1);
                entity.markDirty();
            }
        }

        ItemStack vegStack = entity.items.get(VEG_SLOT);
        if (!vegStack.isEmpty() && entity.vegLevel < MAX_FOOD) {
            if (isVegetable(vegStack)) {
                entity.vegLevel = Math.min(MAX_FOOD, entity.vegLevel + 50);
                vegStack.decrement(1);
                entity.markDirty();
            }
        }
    }

    // --- Feeding API for dinosaurs ---

    public boolean hasMeat() { return meatLevel > 0; }
    public boolean hasVeg() { return vegLevel > 0; }

    public int consumeMeat(int amount) {
        int consumed = Math.min(amount, meatLevel);
        meatLevel -= consumed;
        markDirty();
        return consumed;
    }

    public int consumeVeg(int amount) {
        int consumed = Math.min(amount, vegLevel);
        vegLevel -= consumed;
        markDirty();
        return consumed;
    }

    public int getMeatLevel() { return meatLevel; }
    public int getVegLevel() { return vegLevel; }

    // Use vanilla #minecraft:meat for meat detection. Custom tag for vegetables since
    // vanilla has no generic vegetable/crop-food tag.
    private static final TagKey<Item> MEAT_TAG = net.minecraft.registry.tag.ItemTags.MEAT;
    private static final TagKey<Item> VEG_TAG = TagKey.of(RegistryKeys.ITEM,
            net.minecraft.util.Identifier.of(FossilsArch2Mod.MOD_ID, "feeder_vegetable"));

    private static boolean isMeat(ItemStack stack) {
        return stack.isIn(MEAT_TAG);
    }

    private static boolean isVegetable(ItemStack stack) {
        return stack.isIn(VEG_TAG);
    }
}
