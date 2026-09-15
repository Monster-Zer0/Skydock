package dev.skydock.ship;

import dev.skydock.block.EngineTier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class EngineTierTest {
    @Test void brassPreservesExistingFlightEnvelope() {
        assertEquals(1.0, EngineTier.BRASS.thrustMultiplier);
        assertEquals(1.0, EngineTier.BRASS.fuelEfficiency);
        assertEquals(FlightDynamics.MAX_SPEED, EngineTier.BRASS.maxSpeed);
    }

    @Test void upgradesIncreasePerformanceMonotonicallyWithinSaveLimit() {
        EngineTier[] tiers = EngineTier.values();
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].thrustMultiplier > tiers[i - 1].thrustMultiplier);
            assertTrue(tiers[i].fuelEfficiency > tiers[i - 1].fuelEfficiency);
            assertTrue(tiers[i].maxSpeed > tiers[i - 1].maxSpeed);
        }
        assertEquals(FlightDynamics.MAX_TIER_SPEED, EngineTier.AETHER.maxSpeed);
    }

    @Test void everySpeedLimitIsTwentyPercentAboveTheOriginalEnvelope() {
        assertEquals(.42, EngineTier.BRASS.maxSpeed, 1e-9);
        assertEquals(.48, EngineTier.COMPOUND.maxSpeed, 1e-9);
        assertEquals(.552, EngineTier.TURBINE.maxSpeed, 1e-9);
        assertEquals(.624, EngineTier.AETHER.maxSpeed, 1e-9);
        assertEquals(.42, FlightDynamics.MAX_SPEED, 1e-9);
        assertEquals(.624, FlightDynamics.MAX_TIER_SPEED, 1e-9);
    }
}
