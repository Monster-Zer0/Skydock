package dev.skydock.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic access to chests touching a dock controller, including one connected double-chest half. */
public final class AssemblyInventory {
    private static final List<Direction> SEARCH_ORDER = List.of(
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    private AssemblyInventory() {}

    private record Located(BlockPos pos, Container container) {}
    private record Removal(Located source, int slot, int count) {}

    private static List<Located> sources(ServerLevel level, BlockPos dockPos) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (Direction direction : SEARCH_ORDER) {
            BlockPos adjacent = dockPos.relative(direction);
            if (!(level.getBlockEntity(adjacent) instanceof ChestBlockEntity)) continue;
            positions.add(adjacent.immutable());
            BlockState state = level.getBlockState(adjacent);
            if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                    && state.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                positions.add(adjacent.relative(ChestBlock.getConnectedDirection(state)).immutable());
            }
        }
        List<Located> sources = new ArrayList<>();
        positions.stream().sorted(Comparator.comparingLong(BlockPos::asLong)).forEach(pos -> {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof ChestBlockEntity chest) sources.add(new Located(pos, chest));
        });
        return sources;
    }

    public static Map<Item, Integer> available(ServerLevel level, BlockPos dockPos) {
        Map<Item, Integer> result = new HashMap<>();
        for (Located source : sources(level, dockPos)) for (int slot = 0; slot < source.container.getContainerSize(); slot++) {
            ItemStack stack = source.container.getItem(slot);
            if (!stack.isEmpty()) result.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return Map.copyOf(result);
    }

    /** Simulates the whole extraction before mutating any slot, then executes the same deterministic plan. */
    public static List<ItemStack> extract(ServerLevel level, BlockPos dockPos, Map<Item, Integer> cost) {
        List<Located> sources = sources(level, dockPos);
        List<Removal> plan = new ArrayList<>();
        for (Map.Entry<Item, Integer> requirement : cost.entrySet()) {
            int remaining = requirement.getValue();
            for (Located source : sources) for (int slot = 0; slot < source.container.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = source.container.getItem(slot);
                if (!stack.is(requirement.getKey())) continue;
                int take = Math.min(remaining, stack.getCount());
                plan.add(new Removal(source, slot, take));
                remaining -= take;
            }
            if (remaining > 0) return List.of();
        }
        List<ItemStack> escrow = new ArrayList<>();
        for (Removal removal : plan) {
            ItemStack removed = removal.source.container.removeItem(removal.slot, removal.count);
            if (removed.getCount() != removal.count)
                throw new IllegalStateException("Chest contents changed during dock assembly transaction");
            removal.source.container.setChanged();
            merge(escrow, removed);
        }
        return escrow;
    }

    public static void refund(ServerLevel level, BlockPos dockPos, List<ItemStack> stacks) {
        List<Located> sources = sources(level, dockPos);
        for (ItemStack original : stacks) {
            ItemStack remaining = original.copy();
            for (Located source : sources) insert(source.container, remaining, true);
            for (Located source : sources) insert(source.container, remaining, false);
            if (!remaining.isEmpty()) Containers.dropItemStack(level, dockPos.getX() + .5, dockPos.getY() + 1, dockPos.getZ() + .5, remaining);
        }
    }

    private static void insert(Container container, ItemStack incoming, boolean existingOnly) {
        for (int slot = 0; slot < container.getContainerSize() && !incoming.isEmpty(); slot++) {
            ItemStack current = container.getItem(slot);
            if (existingOnly ? current.isEmpty() || !ItemStack.isSameItemSameComponents(current, incoming) : !current.isEmpty()) continue;
            if (!container.canPlaceItem(slot, incoming)) continue;
            int maximum = Math.min(container.getMaxStackSize(), incoming.getMaxStackSize());
            int room = maximum - current.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, incoming.getCount());
            if (current.isEmpty()) container.setItem(slot, incoming.copyWithCount(moved));
            else current.grow(moved);
            incoming.shrink(moved);
            container.setChanged();
        }
    }

    static void merge(List<ItemStack> into, ItemStack stack) {
        int remaining = stack.getCount();
        for (ItemStack current : into) if (remaining > 0 && ItemStack.isSameItemSameComponents(current, stack)) {
            int room = current.getMaxStackSize() - current.getCount();
            int moved = Math.min(room, remaining);
            if (moved > 0) { current.grow(moved); remaining -= moved; }
        }
        while (remaining > 0) {
            int count = Math.min(stack.getMaxStackSize(), remaining);
            into.add(stack.copyWithCount(count));
            remaining -= count;
        }
    }
}
