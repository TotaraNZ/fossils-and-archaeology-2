package mod.fossilsarch2.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.SuspiciousStoneBlock;
import mod.fossilsarch2.registry.ModBlockEntities;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class SuspiciousStoneBlockEntity extends BlockEntity {

    private static final String LOOT_TABLE_NBT_KEY = "LootTable";
    private static final String LOOT_TABLE_SEED_NBT_KEY = "LootTableSeed";
    private static final String HIT_DIRECTION_NBT_KEY = "hit_direction";
    private static final String ITEM_NBT_KEY = "item";
    private static final int BRUSH_COOLDOWN_TICKS = 10;
    private static final int BRUSH_RESET_TICKS = 40;
    private static final int BRUSH_DECAY_INTERVAL_TICKS = 4;
    private static final int REQUIRED_BRUSHES = 10;
    private static final ResourceKey<LootTable> DEFAULT_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, "archaeology/suspicious_stone"));

    private int brushesCount;
    private long nextDustTime;
    private long nextBrushTime;
    private ItemStack item = ItemStack.EMPTY;
    private Direction hitDirection;
    private ResourceKey<LootTable> lootTable;
    private long lootTableSeed;

    public SuspiciousStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUSPICIOUS_STONE, pos, state);
        setLootTable(DEFAULT_LOOT_TABLE, ThreadLocalRandom.current().nextLong());
    }

    public boolean brush(long currentTime, ServerLevel world, LivingEntity brusher, Direction direction, ItemStack tool) {
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
        setChanged();

        if (brushesCount >= REQUIRED_BRUSHES) {
            finishBrushing(world, brusher, tool);
            return true;
        }

        world.scheduleTick(worldPosition, getBlockState().getBlock(), 2);

        int dustedLevel = getDustedLevel();
        if (previousDustedLevel != dustedLevel) {
            world.setBlock(worldPosition, getBlockState().setValue(SuspiciousStoneBlock.DUSTED, dustedLevel), Block.UPDATE_ALL);
        }

        return false;
    }

    public void scheduledTick(ServerLevel world) {
        if (brushesCount != 0 && world.getGameTime() >= nextDustTime) {
            int previousDustedLevel = getDustedLevel();
            brushesCount = Math.max(0, brushesCount - 2);
            int dustedLevel = getDustedLevel();

            if (previousDustedLevel != dustedLevel) {
                world.setBlock(worldPosition, getBlockState().setValue(SuspiciousStoneBlock.DUSTED, dustedLevel), Block.UPDATE_ALL);
            }

            nextDustTime = world.getGameTime() + BRUSH_DECAY_INTERVAL_TICKS;
            setChanged();
        }

        if (brushesCount == 0) {
            hitDirection = null;
            nextDustTime = 0;
            nextBrushTime = 0;
            setChanged();
            return;
        }

        world.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        nbt.storeNullable(HIT_DIRECTION_NBT_KEY, Direction.CODEC, hitDirection);
        if (!item.isEmpty()) {
            nbt.store(ITEM_NBT_KEY, ItemStack.CODEC, item);
        }
        return nbt;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        lootTable = input.read(LOOT_TABLE_NBT_KEY, LootTable.KEY_CODEC).orElse(null);
        lootTableSeed = input.getLongOr(LOOT_TABLE_SEED_NBT_KEY, 0L);

        if (lootTable != null) {
            item = ItemStack.EMPTY;
        } else {
            item = input.read(ITEM_NBT_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }

        hitDirection = input.read(HIT_DIRECTION_NBT_KEY, Direction.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        if (lootTable != null) {
            output.store(LOOT_TABLE_NBT_KEY, LootTable.KEY_CODEC, lootTable);
            if (lootTableSeed != 0L) {
                output.putLong(LOOT_TABLE_SEED_NBT_KEY, lootTableSeed);
            }
        } else if (!item.isEmpty()) {
            output.store(ITEM_NBT_KEY, ItemStack.CODEC, item);
        }
    }

    public void setLootTable(ResourceKey<LootTable> lootTable, long lootTableSeed) {
        this.lootTable = lootTable;
        this.lootTableSeed = lootTableSeed;
    }

    private void generateItem(ServerLevel world, LivingEntity brusher, ItemStack tool) {
        if (lootTable == null) {
            return;
        }

        LootTable table = world.getServer().reloadableRegistries().getLootTable(lootTable);
        if (brusher instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, lootTable);
        }

        LootParams context = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withLuck(brusher.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, brusher)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.ARCHAEOLOGY);

        List<ItemStack> generatedLoot = new ArrayList<>();
        table.getRandomItems(context, lootTableSeed, generatedLoot::add);
        if (generatedLoot.isEmpty()) {
            item = ItemStack.EMPTY;
        } else {
            if (generatedLoot.size() > 1) {
                FossilsArch2Mod.LOGGER.warn(
                        "Expected max 1 archaeology result from {}, but got {}",
                        lootTable.identifier(),
                        generatedLoot.size());
            }
            item = generatedLoot.getFirst();
        }

        lootTable = null;
        setChanged();
    }

    private void finishBrushing(ServerLevel world, LivingEntity brusher, ItemStack tool) {
        spawnItem(world, brusher, tool);
        world.levelEvent(3008, worldPosition, Block.getId(getBlockState()));
        world.setBlock(worldPosition, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void spawnItem(ServerLevel world, LivingEntity brusher, ItemStack tool) {
        generateItem(world, brusher, tool);
        if (item.isEmpty()) {
            return;
        }

        Direction direction = hitDirection != null ? hitDirection : Direction.UP;
        Vec3 spawnPos = Vec3.atCenterOf(worldPosition).add(
                direction.getStepX() * 0.6,
                direction.getStepY() * 0.6,
                direction.getStepZ() * 0.6);

        ItemEntity itemEntity = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, item.copy());
        itemEntity.setDeltaMovement(Vec3.ZERO);
        world.addFreshEntity(itemEntity);
        item = ItemStack.EMPTY;
        setChanged();
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
