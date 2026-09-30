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
    /** Precomputes the rotation once, for code that transforms many boxes at the same pose. */
    public Transform transform() { double a = Math.toRadians(yaw); return new Transform(x, y, z, Math.cos(a), Math.sin(a)); }
    public record Transform(double x, double y, double z, double cos, double sin) {
        public Vec3 toWorld(Vec3 v) { return new Vec3(cos * v.x - sin * v.z + x, v.y + y, sin * v.x + cos * v.z + z); }
        /** The world box enclosing a rotated local box; the same result as {@link ShipPose#toWorld(AABB)}. */
        public AABB toWorld(AABB b) {
            double cx = (b.minX + b.maxX) / 2, cz = (b.minZ + b.maxZ) / 2, hx = (b.maxX - b.minX) / 2, hz = (b.maxZ - b.minZ) / 2;
            double wx = cos * cx - sin * cz + x, wz = sin * cx + cos * cz + z;
            double ex = Math.abs(cos) * hx + Math.abs(sin) * hz, ez = Math.abs(sin) * hx + Math.abs(cos) * hz;
            return new AABB(wx - ex, b.minY + y, wz - ez, wx + ex, b.maxY + y, wz + ez);
        }
        /** The local box enclosing a world box; the same result as {@link ShipPose#toLocal(AABB)}. */
        public AABB toLocal(AABB b) {
            double cx = (b.minX + b.maxX) / 2 - x, cz = (b.minZ + b.maxZ) / 2 - z, hx = (b.maxX - b.minX) / 2, hz = (b.maxZ - b.minZ) / 2;
            double lx = cos * cx + sin * cz, lz = -sin * cx + cos * cz;
            double ex = Math.abs(cos) * hx + Math.abs(sin) * hz, ez = Math.abs(sin) * hx + Math.abs(cos) * hz;
            return new AABB(lx - ex, b.minY - y, lz - ez, lx + ex, b.maxY - y, lz + ez);
        }
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
