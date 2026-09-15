package dev.skydock.ship;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class ShipSavedData extends SavedData {
    public final Map<UUID, Ship> ships = new LinkedHashMap<>();
    public int nextRegion;
    public static ShipSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ShipSavedData::new, ShipSavedData::load, null), "skydock_ships");
    }
    private static ShipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ShipSavedData data = new ShipSavedData(); data.nextRegion = tag.getInt("NextRegion");
        for (Tag entry : tag.getList("Ships", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) entry;
            Ship ship = Ship.loadPersisted(saved, registries);
            if (!saved.contains("PivotX", Tag.TAG_DOUBLE) || !saved.contains("PivotZ", Tag.TAG_DOUBLE)
                    || ship.revision != saved.getInt("Revision")) data.setDirty();
            ship.pilot = null; data.ships.put(ship.id, ship);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextRegion", nextRegion);
        ListTag shipsTag = new ListTag(); ships.values().forEach(s -> shipsTag.add(s.save(registries, true)));
        tag.put("Ships", shipsTag); return tag;
    }
}
