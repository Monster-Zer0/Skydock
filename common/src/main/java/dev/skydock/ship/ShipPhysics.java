package dev.skydock.ship;

import dev.skydock.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ShipPhysics {
    public static void step(MinecraftServer server, ServerLevel world, Ship ship) {
        if (ship.moored) { ship.velocity = Vec3.ZERO; ship.yawVelocity = 0; ship.cruise = false; ship.appliedThrottle = ship.appliedClimb = 0; return; }
        Vec3 forward = ShipPose.rotate(new Vec3(0, 0, -1), ship.pose.yaw());
        double inertia = Math.max(1, ship.mass / 20000);
        EngineTier poweredTier = null;
        double poweredThrust = 0;
        if (ship.cruise || Math.abs(ship.throttle) > .001 || Math.abs(ship.appliedThrottle) > .001 || Math.abs(ship.steering) > .01) {
            ServerLevel yard = server.getLevel(ShipManager.SHIPYARD);
            for (var entry : ship.blocks.entrySet()) if (entry.getValue().is(SkydockBlocks.ENGINES)
                    && yard.getBlockEntity(ship.yard.offset(entry.getKey())) instanceof EngineBlockEntity engine && engine.burn()) {
                EngineTier tier = engine.tier();
                poweredThrust += tier.thrustMultiplier;
                if (poweredTier == null || tier.ordinal() > poweredTier.ordinal()) poweredTier = tier;
            }
        }
        boolean powered = poweredTier != null;
        if (ship.cruise && !powered) {
            ship.cruise = false;
            var owner = server.getPlayerList().getPlayer(ship.owner);
            if (owner != null) ShipManager.tell(owner, "Cruise disengaged: engines need fuel.");
        }
        double speedLimit = powered ? poweredTier.maxSpeed : FlightDynamics.MAX_SPEED;
        if (ship.cruise) ship.cruiseSpeed = Math.clamp(ship.cruiseSpeed + ship.throttle * .006, -speedLimit, speedLimit);
        double requested = ship.cruise ? FlightDynamics.cruiseThrottle(ship.velocity.dot(forward), ship.cruiseSpeed) * inertia : ship.throttle;
        ship.appliedThrottle = FlightDynamics.approach(ship.appliedThrottle, Math.clamp(requested, -1, 1), .1);
        ship.appliedClimb = FlightDynamics.approach(ship.appliedClimb, ship.climb, .12);
        double thrust = powered ? ship.appliedThrottle * FlightDynamics.ACCELERATION * poweredThrust / inertia : 0;
        Vec3 lateral = new Vec3(ship.velocity.x, 0, ship.velocity.z).subtract(forward.scale(ship.velocity.dot(forward)));
        Vec3 velocity = ship.velocity.multiply(.97, .9, .97).subtract(lateral.scale(.12)).add(forward.scale(thrust));
        double buoyancy = ship.mass <= 0 ? 0 : (ship.lift - ship.mass) / ship.mass;
        double vertical = buoyancy < 0 ? Math.max(-.18, buoyancy * .12) : ship.appliedClimb * Math.min(.16, .035 + buoyancy * .025);
        velocity = new Vec3(velocity.x, velocity.y * .75 + vertical * .25, velocity.z);
        if (velocity.horizontalDistance() > speedLimit) {
            double scale = speedLimit / velocity.horizontalDistance(); velocity = new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
        }
        double hullLength = Math.max(ship.hullBounds().getXsize(), ship.hullBounds().getZsize());
        double desiredTurn = powered ? ship.steering * FlightDynamics.turnRate(ship.mass, hullLength) : 0;
        ship.yawVelocity = FlightDynamics.approach(ship.yawVelocity, desiredTurn, desiredTurn == 0 ? .28 : .18);
        double turn = ship.yawVelocity;
        AABB hull = ship.hullBounds();
        double radius = Math.hypot(Math.max(Math.abs(hull.minX), Math.abs(hull.maxX)), Math.max(Math.abs(hull.minZ), Math.abs(hull.maxZ)));
        double maxTravel = velocity.length() + Math.abs(Math.toRadians(turn)) * radius;
        int steps = Math.max(1, (int) Math.ceil(maxTravel / .2));
        ship.blocked = false;
        for (int i = 0; i < steps; i++) {
            ShipPose next = new ShipPose(ship.pose.x() + velocity.x / steps, ship.pose.y() + velocity.y / steps,
                    ship.pose.z() + velocity.z / steps, ship.pose.yaw() + turn / steps);
            if (collides(world, ship, next)) { ship.blocked = true; velocity = Vec3.ZERO; ship.yawVelocity = 0; ship.cruise = false; ship.appliedThrottle = 0; break; }
            ship.pose = next;
        }
        ship.velocity = velocity;
    }
    public static double installedMaxSpeed(Ship ship) {
        double result = FlightDynamics.MAX_SPEED;
        for (var state : ship.blocks.values()) if (state.is(SkydockBlocks.ENGINES)) result = Math.max(result, EngineTier.from(state).maxSpeed);
        return result;
    }
    private static boolean collides(ServerLevel world, Ship ship, ShipPose pose) {
        AABB bounds = pose.toWorld(ship.hullBounds());
        if (bounds.minY < world.getMinBuildHeight() || bounds.maxY >= world.getMaxBuildHeight() || !world.getWorldBorder().isWithinBounds(bounds)) return true;
        for (VoxelShape terrain : world.getBlockCollisions(null, bounds)) {
            for (AABB box : terrain.toAabbs()) if (!ShipCollision.shapes(ship, pose, box.deflate(1e-5)).isEmpty()) return true;
        }
        for (Ship other : ShipManager.ships(world)) if (other != ship && other.bounds().intersects(bounds)) {
            AABB overlap = other.bounds().intersect(bounds);
            for (VoxelShape box : ShipCollision.shapes(other, other.pose, overlap))
                if (!ShipCollision.shapes(ship, pose, box.bounds().deflate(1e-5)).isEmpty()) return true;
        }
        return false;
    }
}
