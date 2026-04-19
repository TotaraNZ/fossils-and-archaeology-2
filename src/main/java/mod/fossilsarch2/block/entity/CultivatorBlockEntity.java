package mod.fossilsarch2.block.entity;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.screen.CultivatorScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
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

public class CultivatorBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int INVENTORY_SIZE = 3;
    public static final int MAX_COOK_TIME = 600; // 30 seconds

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
        public int getCount() {
            return 4;
        }
    };

    public CultivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CULTIVATOR.get(), pos, state);
    }

    // --- Inventory ---

    @Override public int getContainerSize() { return INVENTORY_SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

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
    public Component getDisplayName() {
        return Component.translatable("block.fossilsarch2.cultivator");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CultivatorScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---

    public static void tick(Level world, BlockPos pos, BlockState state, CultivatorBlockEntity entity) {
        if (world.isClientSide()) return;

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
                    ItemStack fuelStack = entity.items.get(FUEL_SLOT);
                    var remainderTemplate = fuelStack.getItem().getCraftingRemainder();
                    fuelStack.shrink(1);
                    if (remainderTemplate != null && fuelStack.isEmpty()) {
                        entity.items.set(FUEL_SLOT, remainderTemplate.create());
                    }
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
        if (wasBurning != isBurning && state.hasProperty(mod.fossilsarch2.block.CultivatorBlock.LIT)) {
            world.setBlock(pos, state.setValue(mod.fossilsarch2.block.CultivatorBlock.LIT, isBurning), Block.UPDATE_ALL);
        }

        if (dirty) entity.setChanged();
    }

    private boolean canSmelt() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        ItemStack result = getSmeltResult(input);
        if (result == null) return false;

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        if (!output.is(result.getItem())) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void smeltItem() {
        if (!canSmelt()) return;

        ItemStack input = items.get(INPUT_SLOT);
        ItemStack result = getSmeltResult(input);
        if (result == null) return;

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else if (output.is(result.getItem())) {
            output.grow(result.getCount());
        }

        input.shrink(1);
    }

    private ItemStack getSmeltResult(ItemStack input) {
        // DNA → egg conversion: fossilsarch2:{species}_dna → fossilsarch2:{species}_egg
        String species = mod.fossilsarch2.dinosaur.DinosaurUtils.getSpeciesFromItem(input);
        if (species != null) {
            Identifier eggId = Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, species + "_egg");
            Item eggItem = BuiltInRegistries.ITEM.getValue(eggId);
            if (eggItem != Items.AIR) {
                return new ItemStack(eggItem, 1);
            }
        }
        return null;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    private static final TagKey<Item> FUEL_TAG = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "cultivator_fuel"));

    private static int getItemBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(FUEL_TAG)) return 300;
        return 0;
    }
}
