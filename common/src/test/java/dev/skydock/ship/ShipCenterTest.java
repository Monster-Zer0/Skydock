package dev.skydock.ship;

import dev.skydock.data.DockTier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShipCenterTest {
    @BeforeAll static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Ship asymmetricShip() {
        Ship ship = new Ship(); ship.tier = DockTier.SCOUT;
        ship.blocks.put(new BlockPos(12, 0, 3), Blocks.STONE.defaultBlockState());
        ship.blocks.put(new BlockPos(20, 2, 19), Blocks.STONE.defaultBlockState());
        return ship;
    }

    @Test void occupiedBoundsChooseAStableHalfBlockPivotAndInvalidateCachedBounds() {
        Ship ship = asymmetricShip();
        ship.hullBounds();
        ship.initializePivot();
        assertEquals(new Vec3(16.5, 0, 11.5), ship.center());
        assertEquals(new Vec3(-4.5, 0, -8.5), ship.hullBounds().getMinPosition());
        ship.blocks.clear(); ship.blocks.put(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState()); ship.revision++;
        assertEquals(new Vec3(16.5, 0, 11.5), ship.center());
    }

    @Test void legacyRebasePreservesWorldBlocksAnchorAndTheVerticalFrame() {
        Ship ship = asymmetricShip();
        ship.pose = new ShipPose(120.25, 73.75, -48.5, 397);
        ship.previousPose = new ShipPose(119.9, 73.5, -48.1, 396.5);
        ship.pilotAnchor = new Vec3(16.5, 1, 7.5);
        Vec3 oldCenter = new Vec3(16, 0, 16);
        Vec3[] points = {new Vec3(12, 0, 3), new Vec3(20.5, 2, 19.5), ship.pilotAnchor};
        Vec3[] expected = java.util.Arrays.stream(points).map(point -> ship.pose.toWorld(point.subtract(oldCenter))).toArray(Vec3[]::new);
        Vec3 expectedPrevious = ship.previousPose.toWorld(points[1].subtract(oldCenter));
        ship.initializePivot();
        for (int i = 0; i < points.length; i++) assertEquals(0, ship.blockToWorld(points[i]).distanceTo(expected[i]), 1e-9);
        assertEquals(0, ship.previousPose.toWorld(points[1].subtract(ship.center())).distanceTo(expectedPrevious), 1e-9);
        assertEquals(new Vec3(16.5, 1, 7.5), ship.pilotAnchor);
        assertEquals(73.75, ship.pose.y(), 0); assertEquals(397, ship.pose.yaw(), 0);
    }

    @Test void offCenterPivotKeepsTheOriginalDockEnvelopeAndBlockCoordinates() {
        Ship ship = asymmetricShip(); ship.initializePivot();
        Vec3 origin = new Vec3(-30, 64, 90);
        ship.pose = new ShipPose(origin.x + ship.center().x, origin.y, origin.z + ship.center().z, 0);
        AABB world = ship.pose.toWorld(ship.localBounds());
        assertEquals(-30, world.minX, 0); assertEquals(64, world.minY, 0); assertEquals(90, world.minZ, 0);
        assertEquals(2, world.maxX, 0); assertEquals(84, world.maxY, 0); assertEquals(122, world.maxZ, 0);
        Vec3 block = new Vec3(12.5, 1, 3.5);
        assertEquals(0, ship.worldToBlock(ship.blockToWorld(block)).distanceTo(block), 1e-9);
    }

    @Test void savedHeadersCarryPivotAndOnlyPersistedLegacyShipsAreMigrated() {
        HolderLookup.Provider registries = HolderLookup.Provider.create(Stream.of(BuiltInRegistries.BLOCK.asLookup()));
        Ship source = asymmetricShip(); source.dock = BlockPos.ZERO; source.yard = BlockPos.ZERO;
        source.pose = new ShipPose(100, 70, -20, 45); source.previousPose = source.pose;
        CompoundTag legacy = source.save(registries, true); legacy.remove("PivotX"); legacy.remove("PivotZ");
        Ship migrated = Ship.loadPersisted(legacy, registries);
        assertEquals(new Vec3(16.5, 0, 11.5), migrated.center());
        ShipPose migratedPose = migrated.pose;
        Ship loadedAgain = Ship.loadPersisted(migrated.save(registries, true), registries);
        assertEquals(migratedPose, loadedAgain.pose);
        assertEquals(migrated.center(), loadedAgain.center());
        assertEquals(0, migrated.blockToWorld(new Vec3(12.5, 1, 3.5)).distanceTo(loadedAgain.blockToWorld(new Vec3(12.5, 1, 3.5))), 1e-9);
        CompoundTag header = migrated.save(registries, false);
        assertEquals(16.5, header.getDouble("PivotX"), 0); assertEquals(11.5, header.getDouble("PivotZ"), 0);
        header.remove("PivotX"); header.remove("PivotZ");
        Ship incompleteNetworkHeader = Ship.load(header, registries);
        assertEquals(new Vec3(16, 0, 16), incompleteNetworkHeader.center());
        assertEquals(new ShipPose(header.getDouble("X"), header.getDouble("Y"), header.getDouble("Z"), header.getDouble("Yaw")), incompleteNetworkHeader.pose);
    }
}
