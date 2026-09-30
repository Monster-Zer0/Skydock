package dev.skydock.ship;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipPoseTest {
    @Test void positionsRoundTripAcrossYawAndTranslation() {
        for (double yaw : new double[]{0, 45, 90, 179, 270, -135, 1080}) {
            ShipPose pose = new ShipPose(1200.75, -32.5, -920.25, yaw);
            for (Vec3 point : new Vec3[]{Vec3.ZERO, new Vec3(16, 20, 32), new Vec3(-48, .125, -48)}) {
                assertEquals(0, pose.toLocal(pose.toWorld(point)).distanceTo(point), 1e-9);
            }
        }
    }
    @Test void yawPreservesDistanceAndAltitude() {
        ShipPose pose = new ShipPose(5, 60, -10, 37);
        Vec3 a = new Vec3(-3, 2, 4), b = new Vec3(9, 17, -8);
        assertEquals(a.distanceTo(b), pose.toWorld(a).distanceTo(pose.toWorld(b)), 1e-9);
        assertEquals(62, pose.toWorld(a).y, 1e-9);
    }
    @Test void quarterTurnExchangesHorizontalBounds() {
        AABB world = new ShipPose(10, 40, 20, 90).toWorld(new AABB(-2, 0, -8, 2, 5, 8));
        assertEquals(16, world.getXsize(), 1e-9); assertEquals(4, world.getZsize(), 1e-9);
        assertEquals(5, world.getYsize(), 1e-9); assertEquals(10, world.getCenter().x, 1e-9);
    }
    @Test void rotatedBoundsContainEveryCorner() {
        ShipPose pose = new ShipPose(-77, 83, 99, 23.5); AABB source = new AABB(-16, 0, -16, 16, 20, 16);
        AABB bounds = pose.toWorld(source).inflate(1e-8);
        for (int i = 0; i < 8; i++) assertTrue(bounds.contains(pose.toWorld(new Vec3((i & 1) == 0 ? -16 : 16, (i & 2) == 0 ? 0 : 20, (i & 4) == 0 ? -16 : 16))));
    }
    @Test void precomputedTransformMatchesCornerTransform() {
        AABB[] boxes = {new AABB(-2, 0, -8, 2, 5, 8), new AABB(3.25, -1, 7.5, 4.25, 0, 8.5), new AABB(-40, 2, 11, -39.5, 3.5, 30)};
        for (double yaw : new double[]{0, 17.5, 90, 135, -63, 359.9, 721}) {
            ShipPose pose = new ShipPose(-77.5, 83, 1099.25, yaw);
            ShipPose.Transform transform = pose.transform();
            for (AABB box : boxes) {
                assertBoxEquals(pose.toWorld(box), transform.toWorld(box));
                assertBoxEquals(pose.toLocal(box), transform.toLocal(box));
            }
        }
    }
    private static void assertBoxEquals(AABB expected, AABB actual) {
        assertEquals(expected.minX, actual.minX, 1e-9); assertEquals(expected.minY, actual.minY, 1e-9); assertEquals(expected.minZ, actual.minZ, 1e-9);
        assertEquals(expected.maxX, actual.maxX, 1e-9); assertEquals(expected.maxY, actual.maxY, 1e-9); assertEquals(expected.maxZ, actual.maxZ, 1e-9);
    }
    @Test void carryingRetainsTheSameDeckLocation() {
        ShipPose before = new ShipPose(0, 80, 0, 12), after = new ShipPose(1, 80.1, -2, 14);
        Vec3 localFeet = new Vec3(-5, 1, 7);
        Vec3 carried = after.toWorld(before.toLocal(before.toWorld(localFeet)));
        assertEquals(0, after.toLocal(carried).distanceTo(localFeet), 1e-9);
    }
}
