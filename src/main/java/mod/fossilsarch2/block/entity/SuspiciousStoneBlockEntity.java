package mod.fossilsarch2.block.entity;

import java.util.concurrent.ThreadLocalRandom;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.SuspiciousStoneBlock;
import mod.fossilsarch2.registry.ModBlockEntities;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SuspiciousStoneBlockEntity extends BlockEntity {

    private static final String LOOT_TABLE_NBT_KEY = "LootTable";
    private static final String LOOT_TABLE_SEED_NBT_KEY = "LootTableSeed";
    private static final String HIT_DIRECTION_NBT_KEY = "hit_direction";
    private static final String ITEM_NBT_KEY = "item";
    private static final int BRUSH_COOLDOWN_TICKS = 10;
    private static final int BRUSH_RESET_TICKS = 40;
    private static final int BRUSH_DECAY_INTERVAL_TICKS = 4;
    private static final int REQUIRED_BRUSHES = 10;
    private static final RegistryKey<LootTable> DEFAULT_LOOT_TABLE = RegistryKey.of(
            RegistryKeys.LOOT_TABLE,
            Identifier.of(FossilsArch2Mod.MOD_ID, "archaeology/suspicious_stone"));

    private int brushesCount;
    private long nextDustTime;
    private long nextBrushTime;
    private ItemStack item = ItemStack.EMPTY;
    private Direction hitDirection;
    private RegistryKey<LootTable> lootTable;
    private long lootTableSeed;

    public SuspiciousStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUSPICIOUS_STONE, pos, state);
        setLootTable(DEFAULT_LOOT_TABLE, ThreadLocalRandom.current().nextLong());
    }

    public boolean brush(long currentTime, ServerWorld world, LivingEntity brusher, Direction direction, ItemStack tool) {
        if (hitDirection == null) {
            hitDirection = direction;
        }

        nextDustTime = currentTime + BRUSH_RESET_TICKS;
        if (currentTime < nextBrushTime) {
            return false;
        }

        nextBrushTime = currentTime + BRUSH_COOLDOWN_TICKS;
        generateItem(world, brusher, tool);

        int previousDustedLevel = getDustedLevel();
        brushesCount++;
        markDirty();

        if (brushesCount >= REQUIRED_BRUSHES) {
            finishBrushing(world, brusher, tool);
            return true;
        }

        world.scheduleBlockTick(pos, getCachedState().getBlock(), 2);

        int dustedLevel = getDustedLevel();
        if (previousDustedLevel != dustedLevel) {
            world.setBlockState(pos, getCachedState().with(SuspiciousStoneBlock.DUSTED, dustedLevel), Block.NOTIFY_ALL);
        }

        return false;
    }

    public void scheduledTick(ServerWorld world) {
        if (brushesCount != 0 && world.getTime() >= nextDustTime) {
            int previousDustedLevel = getDustedLevel();
            brushesCount = Math.max(0, brushesCount - 2);
            int dustedLevel = getDustedLevel();

            if (previousDustedLevel != dustedLevel) {
                world.setBlockState(pos, getCachedState().with(SuspiciousStoneBlock.DUSTED, dustedLevel), Block.NOTIFY_ALL);
            }

            nextDustTime = world.getTime() + BRUSH_DECAY_INTERVAL_TICKS;
            markDirty();
        }

        if (brushesCount == 0) {
            hitDirection = null;
            nextDustTime = 0;
            nextBrushTime = 0;
            markDirty();
            return;
        }

        world.scheduleBlockTick(pos, getCachedState().getBlock(), 2);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = super.toInitialChunkDataNbt(registries);
        nbt.putNullable(HIT_DIRECTION_NBT_KEY, Direction.CODEC, hitDirection);
        if (!item.isEmpty()) {
            nbt.put(ITEM_NBT_KEY, item.toNbt(registries));
        }
        return nbt;
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        if (readLootTableFromNbt(nbt)) {
            item = ItemStack.EMPTY;
        } else {
            item = nbt.contains(ITEM_NBT_KEY)
                    ? ItemStack.fromNbt(registries, nbt.get(ITEM_NBT_KEY)).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
        }

        hitDirection = nbt.get(HIT_DIRECTION_NBT_KEY, Direction.CODEC).orElse(null);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);

        if (!writeLootTableToNbt(nbt) && !item.isEmpty()) {
            nbt.put(ITEM_NBT_KEY, item.toNbt(registries));
        }
    }

    public void setLootTable(RegistryKey<LootTable> lootTable, long lootTableSeed) {
        this.lootTable = lootTable;
        this.lootTableSeed = lootTableSeed;
    }

    private void generateItem(ServerWorld world, LivingEntity brusher, ItemStack tool) {
        if (lootTable == null) {
            return;
        }

        LootTable table = world.getServer().getReloadableRegistries().getLootTable(lootTable);
        if (brusher instanceof ServerPlayerEntity serverPlayer) {
            Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger(serverPlayer, lootTable);
        }

        LootWorldContext context = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                .luck(brusher.getLuck())
                .add(LootContextParameters.THIS_ENTITY, brusher)
                .add(LootContextParameters.TOOL, tool)
                .build(LootContextTypes.ARCHAEOLOGY);

        ObjectArrayList<ItemStack> generatedLoot = table.generateLoot(context, lootTableSeed);
        if (generatedLoot.isEmpty()) {
            item = ItemStack.EMPTY;
        } else {
            if (generatedLoot.size() > 1) {
                FossilsArch2Mod.LOGGER.warn(
                        "Expected max 1 archaeology result from {}, but got {}",
                        lootTable.getValue(),
                        generatedLoot.size());
            }
            item = generatedLoot.getFirst();
        }

        lootTable = null;
        markDirty();
    }

    private void finishBrushing(ServerWorld world, LivingEntity brusher, ItemStack tool) {
        spawnItem(world, brusher, tool);
        world.syncWorldEvent(3008, pos, Block.getRawIdFromState(getCachedState()));
        world.setBlockState(pos, Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
    }

    private void spawnItem(ServerWorld world, LivingEntity brusher, ItemStack tool) {
        generateItem(world, brusher, tool);
        if (item.isEmpty()) {
            return;
        }

        Direction direction = hitDirection != null ? hitDirection : Direction.UP;
        Vec3d spawnPos = Vec3d.ofCenter(pos).add(
                direction.getOffsetX() * 0.6,
                direction.getOffsetY() * 0.6,
                direction.getOffsetZ() * 0.6);

        ItemEntity itemEntity = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, item.copy());
        itemEntity.setVelocity(Vec3d.ZERO);
        world.spawnEntity(itemEntity);
        item = ItemStack.EMPTY;
        markDirty();
    }

    private boolean readLootTableFromNbt(NbtCompound nbt) {
        lootTable = nbt.get(LOOT_TABLE_NBT_KEY, LootTable.TABLE_KEY).orElse(null);
        lootTableSeed = nbt.getLong(LOOT_TABLE_SEED_NBT_KEY, 0L);
        return lootTable != null;
    }

    private boolean writeLootTableToNbt(NbtCompound nbt) {
        if (lootTable == null) {
            return false;
        }

        nbt.put(LOOT_TABLE_NBT_KEY, LootTable.TABLE_KEY, lootTable);
        if (lootTableSeed != 0L) {
            nbt.putLong(LOOT_TABLE_SEED_NBT_KEY, lootTableSeed);
        }
        return true;
    }

    private int getDustedLevel() {
        if (brushesCount == 0) {
            return 0;
        }
        if (brushesCount < 3) {
            return 1;
        }
        if (brushesCount < 6) {
            return 2;
        }
        return 3;
    }
}
