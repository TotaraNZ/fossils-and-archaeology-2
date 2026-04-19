package mod.fossilsarch2.block.entity;

import java.util.ArrayList;
import java.util.List;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.AnalyserBlock;
import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import mod.fossilsarch2.screen.AnalyserScreenHandler;

public class AnalyserBlockEntity extends BlockEntity implements MenuProvider, Container {

    public static final int INPUT_START = 0;
    public static final int INPUT_END = 8; // 9 input slots (0-8)
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 12; // 4 output slots (9-12)
    public static final int INVENTORY_SIZE = 13;
    public static final int MAX_PROCESS_TIME = 200; // 10 seconds

    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int processTime = 0;

    private final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> processTime;
                case 1 -> MAX_PROCESS_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) processTime = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public AnalyserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANALYSER.get(), pos, state);
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
        output.putInt("ProcessTime", processTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        processTime = input.getIntOr("ProcessTime", 0);
    }

    // --- Screen ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fossilsarch2.analyser");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new AnalyserScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---

    public static void tick(Level world, BlockPos pos, BlockState state, AnalyserBlockEntity entity) {
        if (world.isClientSide()) return;

        boolean dirty = false;

        // Find an input slot to process
        int inputSlot = entity.findNextInput();
        if (inputSlot >= 0) {
            entity.processTime++;
            dirty = true;

            if (entity.processTime >= MAX_PROCESS_TIME) {
                entity.processTime = 0;
                entity.processItem(inputSlot);
            }

            if (state.hasProperty(AnalyserBlock.LIT) && !state.getValue(AnalyserBlock.LIT)) {
                world.setBlock(pos, state.setValue(AnalyserBlock.LIT, true), Block.UPDATE_ALL);
            }
        } else {
            if (entity.processTime > 0) {
                entity.processTime = 0;
                dirty = true;
            }
            if (state.hasProperty(AnalyserBlock.LIT) && state.getValue(AnalyserBlock.LIT)) {
                world.setBlock(pos, state.setValue(AnalyserBlock.LIT, false), Block.UPDATE_ALL);
            }
        }

        if (dirty) entity.setChanged();
    }

    private int findNextInput() {
        for (int i = INPUT_START; i <= INPUT_END; i++) {
            if (!items.get(i).isEmpty() && isValidInput(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidInput(ItemStack stack) {
        if (stack.is(ModItems.BIO_FOSSIL)) return true;
        // Check if it's a dinosaur meat item
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals(FossilsArch2Mod.MOD_ID) && id.getPath().endsWith("_meat");
    }

    private void processItem(int slot) {
        ItemStack input = items.get(slot);
        if (input.isEmpty()) return;

        ItemStack result = getProcessResult(input);
        if (result == null || result.isEmpty()) return;

        // Try to place in output slots
        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack output = items.get(i);
            if (output.isEmpty()) {
                items.set(i, result.copy());
                input.shrink(1);
                return;
            } else if (output.is(result.getItem()) &&
                    output.getCount() + result.getCount() <= output.getMaxStackSize()) {
                output.grow(result.getCount());
                input.shrink(1);
                return;
            }
        }
    }

    private ItemStack getProcessResult(ItemStack input) {
        if (input.is(ModItems.BIO_FOSSIL)) {
            // Random DNA from any registered dinosaur
            var registry = level.registryAccess().lookupOrThrow(mod.fossilsarch2.dinosaur.Dinosaurs.REGISTRY_KEY);
            List<Identifier> ids = new ArrayList<>(registry.keySet());
            if (ids.isEmpty()) return null;
            Identifier randomId = ids.get(level.getRandom().nextInt(ids.size()));
            Item dnaItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, randomId.getPath() + "_dna"));
            if (dnaItem != Items.AIR) {
                return new ItemStack(dnaItem, 1);
            }
        }

        // Meat → species DNA
        String species = mod.fossilsarch2.dinosaur.DinosaurUtils.getSpeciesFromItem(input);
        if (species != null) {
            Item dnaItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, species + "_dna"));
            if (dnaItem != Items.AIR) {
                return new ItemStack(dnaItem, 4);
            }
        }

        return null;
    }
}
