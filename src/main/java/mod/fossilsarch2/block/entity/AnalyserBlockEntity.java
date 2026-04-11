package mod.fossilsarch2.block.entity;

import java.util.ArrayList;
import java.util.List;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.AnalyserBlock;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.registry.DinosaurRegistry;
import mod.fossilsarch2.registry.ModBlockEntities;
import mod.fossilsarch2.registry.ModItems;
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
import mod.fossilsarch2.screen.AnalyserScreenHandler;

public class AnalyserBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Inventory {

    public static final int INPUT_START = 0;
    public static final int INPUT_END = 8; // 9 input slots (0-8)
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 12; // 4 output slots (9-12)
    public static final int INVENTORY_SIZE = 13;
    public static final int MAX_PROCESS_TIME = 200; // 10 seconds

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int processTime = 0;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 2;
        }
    };

    public AnalyserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANALYSER, pos, state);
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
        nbt.putInt("ProcessTime", processTime);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, items, registries);
        processTime = nbt.getInt("ProcessTime", 0);
    }

    // --- Screen ---

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.fossilsarch2.analyser");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new AnalyserScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    // --- Tick ---

    public static void tick(World world, BlockPos pos, BlockState state, AnalyserBlockEntity entity) {
        if (world.isClient()) return;

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

            if (state.contains(AnalyserBlock.LIT) && !state.get(AnalyserBlock.LIT)) {
                world.setBlockState(pos, state.with(AnalyserBlock.LIT, true), Block.NOTIFY_ALL);
            }
        } else {
            if (entity.processTime > 0) {
                entity.processTime = 0;
                dirty = true;
            }
            if (state.contains(AnalyserBlock.LIT) && state.get(AnalyserBlock.LIT)) {
                world.setBlockState(pos, state.with(AnalyserBlock.LIT, false), Block.NOTIFY_ALL);
            }
        }

        if (dirty) entity.markDirty();
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
        if (stack.isOf(ModItems.BIO_FOSSIL)) return true;
        // Check if it's a dinosaur meat item
        Identifier id = Registries.ITEM.getId(stack.getItem());
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
                input.decrement(1);
                return;
            } else if (output.isOf(result.getItem()) &&
                    output.getCount() + result.getCount() <= output.getMaxCount()) {
                output.increment(result.getCount());
                input.decrement(1);
                return;
            }
        }
    }

    private ItemStack getProcessResult(ItemStack input) {
        if (input.isOf(ModItems.BIO_FOSSIL)) {
            // Random DNA from any registered dinosaur
            List<Dinosaur> dinos = new ArrayList<>(DinosaurRegistry.all().values());
            if (dinos.isEmpty()) return null;
            Dinosaur random = dinos.get(world.getRandom().nextInt(dinos.size()));
            Item dnaItem = Registries.ITEM.get(Identifier.of(FossilsArch2Mod.MOD_ID, random.id + "_dna"));
            if (dnaItem != Items.AIR) {
                return new ItemStack(dnaItem, 1);
            }
        }

        // Meat → species DNA
        String species = mod.fossilsarch2.dinosaur.DinosaurUtils.getSpeciesFromItem(input);
        if (species != null) {
            Item dnaItem = Registries.ITEM.get(Identifier.of(FossilsArch2Mod.MOD_ID, species + "_dna"));
            if (dnaItem != Items.AIR) {
                return new ItemStack(dnaItem, 4);
            }
        }

        return null;
    }
}
