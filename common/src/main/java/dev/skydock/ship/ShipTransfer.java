package dev.skydock.ship;

import dev.skydock.Skydock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Journal is flushed before either world is edited. Interrupted transfers can be replayed. */
public final class ShipTransfer {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    public static boolean changing;

    public static void snapshot(Ship ship, ServerLevel level, BlockPos origin) {
        ship.blockEntities.clear();
        for (BlockPos p : ship.blocks.keySet()) {
            BlockEntity entity = level.getBlockEntity(origin.offset(p));
            if (entity != null) ship.blockEntities.put(p, entity.saveWithFullMetadata(level.registryAccess()));
        }
    }
    public static void flush(MinecraftServer server) {
        ShipSavedData.get(server).setDirty();
        // Flush the journal and chunk writes before advancing a transfer phase.
        server.saveEverything(false, true, true);
    }
    public static void complete(MinecraftServer server, Ship ship) {
        if (ship.phase.equals("active")) return;
        ServerLevel yard = server.getLevel(ShipManager.SHIPYARD);
        ServerLevel destination = server.getLevel(ship.transferDimension);
        if (yard == null || destination == null) throw new IllegalStateException("Transfer dimension missing; journal retained");
        changing = true;
        try {
            if (ship.phase.equals("launching")) {
                paste(ship, yard, ship.yard);
                flush(server);
                clear(ship, destination, ship.transferOrigin);
                ship.phase = "active";
                ship.transferOrigin = null;
                flush(server);
            } else if (ship.phase.equals("redocking")) {
                paste(ship, destination, ship.transferOrigin);
                flush(server);
                clear(ship, yard, ship.yard);
                ShipSavedData.get(server).ships.remove(ship.id);
                ShipManager.releaseTickets(server, ship);
                flush(server);
            } else throw new IllegalStateException("Unknown transfer phase " + ship.phase);
        } finally { changing = false; }
    }
    private static void clear(Ship ship, ServerLevel level, BlockPos origin) {
        for (BlockPos p : ship.blocks.keySet()) {
            BlockPos at = origin.offset(p);
            // Removing the BE before its block avoids Container.onRemove duplicating inventory drops.
            level.removeBlockEntity(at);
            level.setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
        }
        for (BlockPos p : ship.blocks.keySet()) level.updateNeighborsAt(origin.offset(p), Blocks.AIR);
    }
    private static void paste(Ship ship, ServerLevel level, BlockPos origin) {
        ship.blocks.forEach((p, state) -> {
            BlockPos at = origin.offset(p); level.removeBlockEntity(at);
            level.setBlock(at, state, FLAGS);
        });
        ship.blockEntities.forEach((p, data) -> {
            BlockPos at = origin.offset(p); CompoundTag tag = data.copy();
            tag.putInt("x", at.getX()); tag.putInt("y", at.getY()); tag.putInt("z", at.getZ());
            BlockEntity entity = BlockEntity.loadStatic(at, level.getBlockState(at), tag, level.registryAccess());
            if (entity == null) throw new IllegalStateException("Could not restore block entity at " + at);
            level.setBlockEntity(entity); entity.setChanged();
        });
        for (BlockPos p : ship.blocks.keySet()) level.updateNeighborsAt(origin.offset(p), ship.state(p).getBlock());
    }
}
