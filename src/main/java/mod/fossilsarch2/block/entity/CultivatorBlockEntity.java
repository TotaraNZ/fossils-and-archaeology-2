package mod.fossilsarch2.block.entity;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.screen.CultivatorScreenHandler;
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
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CultivatorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Inventory {

    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;
    public static final int MAX_COOK_TIME = 600; // 30 seconds

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
        public int size() {
            return 4;
        }
    };

    public CultivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CULTIVATOR, pos, state);
    }

    // --- Inventory ---

    @Override public int size() { return INVENTORY_SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

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
    public Text getDisplayName() {
        return Text.translatable("block.fossilsarch2.cultivator");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CultivatorScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---

    public static void tick(World world, BlockPos pos, BlockState state, CultivatorBlockEntity entity) {
        if (world.isClient()) return;

        boolean wasBurning = entity.burnTime > 0;
        boolean dirty = false;

        if (entity.burnTime > 0) {
            entity.burnTime--;
            dirty = true;
        }

        if (entity.canSmelt()) {
            // Try to start burning fuel if not already
            if (entity.burnTime == 0) {
                int fuelTime = getItemBurnTime(entity.items.get(FUEL_SLOT));
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
            } else {
                if (entity.cookTime > 0) {
                    entity.cookTime = 0;
                    dirty = true;
                }
            }
        } else {
            if (entity.cookTime > 0) {
                entity.cookTime = 0;
                dirty = true;
            }
        }

        boolean isBurning = entity.burnTime > 0;
        if (wasBurning != isBurning && state.contains(mod.fossilsarch2.block.CultivatorBlock.LIT)) {
            world.setBlockState(pos, state.with(mod.fossilsarch2.block.CultivatorBlock.LIT, isBurning), net.minecraft.block.Block.NOTIFY_ALL);
        }

        if (dirty) entity.markDirty();
    }

    private boolean canSmelt() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        ItemStack result = getSmeltResult(input);
        if (result == null) return false;

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        if (!output.isOf(result.getItem())) return false;
        return output.getCount() + result.getCount() <= output.getMaxCount();
    }

    private void smeltItem() {
        if (!canSmelt()) return;

        ItemStack input = items.get(INPUT_SLOT);
        ItemStack result = getSmeltResult(input);
        if (result == null) return;

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else if (output.isOf(result.getItem())) {
            output.increment(result.getCount());
        }

        input.decrement(1);
    }

    private ItemStack getSmeltResult(ItemStack input) {
        // DNA → egg conversion: fossilsarch2:{species}_dna → fossilsarch2:{species}_egg
        String species = mod.fossilsarch2.dinosaur.DinosaurUtils.getSpeciesFromItem(input);
        if (species != null) {
            Identifier eggId = Identifier.of(FossilsArch2Mod.MOD_ID, species + "_egg");
            Item eggItem = Registries.ITEM.get(eggId);
            if (eggItem != Items.AIR) {
                return new ItemStack(eggItem, 1);
            }
        }
        return null;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    private static final net.minecraft.registry.tag.TagKey<Item> FUEL_TAG = net.minecraft.registry.tag.TagKey.of(
            net.minecraft.registry.RegistryKeys.ITEM,
            net.minecraft.util.Identifier.of(FossilsArch2Mod.MOD_ID, "cultivator_fuel"));

    private static int getItemBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.isIn(FUEL_TAG)) return 300;
        return 0;
    }
}
