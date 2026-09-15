package dev.skydock.ship;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShipMovementFrameTest {
    private static final UUID SHIP = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test void rebasesHistoricalClientPositionIntoCurrentServerPose() {
        ShipMotion history = history();
        ShipMovementFrame.Frame frame = new ShipMovementFrame.Frame(7, SHIP, 10,
                new Vec3(2, 1, -3), false);

        Vec3 result = ShipMovementFrame.rebase(frame, SHIP, 6, history,
                new ShipPose(13, 4, 7, 90), Vec3.ZERO, new Vec3(12, 1, 2));

        assertEquals(new Vec3(16, 5, 9), result);
    }

    @Test void rejectsStaleNonFiniteWrongShipAndReplayFrames() {
        ShipMotion history = history();
        ShipPose current = new ShipPose(13, 4, 7, 90);
        Vec3 packet = new Vec3(12, 1, 2);
        ShipMovementFrame.Frame valid = new ShipMovementFrame.Frame(7, SHIP, 10, new Vec3(2, 1, -3), false);

        assertNull(ShipMovementFrame.rebase(new ShipMovementFrame.Frame(8, SHIP, 2, valid.localFeet(), false),
                SHIP, 6, history, current, Vec3.ZERO, packet));
        assertNull(ShipMovementFrame.rebase(new ShipMovementFrame.Frame(8, SHIP, 10,
                        new Vec3(Double.NaN, 1, -3), false), SHIP, 6, history, current, Vec3.ZERO, packet));
        assertNull(ShipMovementFrame.rebase(valid, OTHER, 6, history, current, Vec3.ZERO, packet));
        assertNull(ShipMovementFrame.rebase(valid, SHIP, 7, history, current, Vec3.ZERO, packet));
        assertNull(ShipMovementFrame.rebase(valid, SHIP, 6, history, current, Vec3.ZERO,
                new Vec3(Double.POSITIVE_INFINITY, 1, 2)));
    }

    @Test void receiveSequenceDropsReplayEvenWhenShipTimeRepeats() {
        ShipAttachment attachment = new ShipAttachment();
        ShipMovementFrame.Frame newest = new ShipMovementFrame.Frame(9, SHIP, 10, new Vec3(1, 1, 1), false);
        attachment.receive(newest);
        attachment.receive(new ShipMovementFrame.Frame(8, SHIP, 10, new Vec3(2, 1, 1), false));
        assertSame(newest, attachment.consume());
        assertNull(attachment.consume());
    }

    @Test void rejectsMetadataThatDoesNotDescribeTheVanillaPacket() {
        ShipMovementFrame.Frame frame = new ShipMovementFrame.Frame(7, SHIP, 10,
                new Vec3(2, 1, -3), false);
        assertNull(ShipMovementFrame.rebase(frame, SHIP, 6, history(),
                new ShipPose(13, 4, 7, 90), Vec3.ZERO, new Vec3(11.8, 1, 2)));
    }

    @Test void departureUsesTheSameBoundedRebaseAndRetainsItsSignal() {
        ShipMovementFrame.Frame frame = new ShipMovementFrame.Frame(7, SHIP, 10,
                new Vec3(2, 1, -3), true);
        assertTrue(frame.departing());
        assertNotNull(ShipMovementFrame.rebase(frame, SHIP, 6, history(),
                new ShipPose(13, 4, 7, 90), Vec3.ZERO, new Vec3(12, 1, 2)));
    }

    private static ShipMotion history() {
        ShipMotion history = new ShipMotion();
        history.accept(10, new ShipPose(10, 0, 5, 0), new Vec3(1, 0, 0), 0);
        history.accept(12, new ShipPose(12, 0, 5, 0), new Vec3(1, 0, 0), 0);
        return history;
    }
}
