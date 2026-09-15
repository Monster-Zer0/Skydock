package dev.skydock.ship;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Validates that a custom deck frame describes the adjacent vanilla movement packet. */
public final class ShipMovementFrame {
    private static final double PACKET_MATCH_TOLERANCE = .125;

    private ShipMovementFrame() {}

    public record Frame(long sequence, UUID ship, double shipTime, Vec3 localFeet, boolean departing) {
        public static Frame none(long sequence) { return new Frame(sequence, null, 0, Vec3.ZERO, false); }
        public Frame withSequence(long value) { return new Frame(value, ship, shipTime, localFeet, departing); }
        public boolean finite() {
            return Double.isFinite(shipTime) && Double.isFinite(localFeet.x)
                    && Double.isFinite(localFeet.y) && Double.isFinite(localFeet.z);
        }
    }

    public static Vec3 rebase(Frame frame, UUID attachedShip, long lastAcceptedSequence,
                              ShipMotion history, ShipPose currentPose, Vec3 center, Vec3 packetPosition) {
        if (frame == null || frame.ship() == null || !frame.ship().equals(attachedShip)
                || frame.sequence() <= lastAcceptedSequence || !frame.finite()
                || !history.contains(frame.shipTime()) || !finite(packetPosition)) return null;
        Vec3 relative = frame.localFeet().subtract(center);
        Vec3 historicalPosition = history.sample(frame.shipTime()).toWorld(relative);
        if (historicalPosition.distanceToSqr(packetPosition) > PACKET_MATCH_TOLERANCE * PACKET_MATCH_TOLERANCE)
            return null;
        return currentPose.toWorld(relative);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }
}
