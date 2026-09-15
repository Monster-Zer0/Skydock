package dev.skydock.ship;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Local coordinates rotate around the ship's persisted pivot; yaw is in degrees. */
public record ShipPose(double x, double y, double z, double yaw) {
    public ShipPose interpolate(ShipPose next, double fraction) {
        double turn = ((next.yaw - yaw) % 360 + 540) % 360 - 180;
        return new ShipPose(x + (next.x - x) * fraction, y + (next.y - y) * fraction,
                z + (next.z - z) * fraction, yaw + turn * fraction);
    }
    public Vec3 toWorld(Vec3 local) { return rotate(local, yaw).add(x, y, z); }
    public Vec3 toLocal(Vec3 world) { return rotate(world.subtract(x, y, z), -yaw); }
    public static Vec3 rotate(Vec3 v, double degrees) {
        double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(c * v.x - s * v.z, v.y, s * v.x + c * v.z);
    }
    public AABB toWorld(AABB box) { return transformBox(box, true); }
    public AABB toLocal(AABB box) { return transformBox(box, false); }
    private AABB transformBox(AABB b, boolean world) {
        AABB result = null;
        for (int i = 0; i < 8; i++) {
            Vec3 v = new Vec3((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            v = world ? toWorld(v) : toLocal(v);
            AABB point = new AABB(v, v);
            result = result == null ? point : result.minmax(point);
        }
        return result;
    }
}
