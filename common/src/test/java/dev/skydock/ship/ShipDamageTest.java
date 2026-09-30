package dev.skydock.ship;

import dev.skydock.data.CollisionRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipDamageTest {
    private static final CollisionRules RULES = CollisionRules.DEFAULT;

    @Test void impactsAtOrBelowTheSafeSpeedBreakNothing() {
        assertEquals(0, ShipDamage.budget(RULES, 0, 50_000));
        assertEquals(0, ShipDamage.budget(RULES, -1.4, 50_000));
        assertFalse(ShipDamage.breaks(RULES, 0, 0));
    }

    @Test void fasterAndHeavierHullsBreakMoreBlocks() {
        assertEquals(1, ShipDamage.budget(RULES, .5, 5_000));
        assertTrue(ShipDamage.budget(RULES, 6, 5_000) > ShipDamage.budget(RULES, 2, 5_000));
        assertTrue(ShipDamage.budget(RULES, 6, 45_000) > ShipDamage.budget(RULES, 6, 5_000));
        assertEquals(RULES.maxBlocks(), ShipDamage.budget(RULES, 500, 1e7));
        assertEquals(0, ShipDamage.budget(new CollisionRules(3, 1.5, 1, 0, 1, true), 9, 45_000));
    }

    @Test void harderBlocksNeedAFasterImpact() {
        // Canvas lift cells (1) give way before planks (2), and those before engines (4).
        assertTrue(ShipDamage.breaks(RULES, 1.5, 1));
        assertFalse(ShipDamage.breaks(RULES, 1.5, 2));
        assertTrue(ShipDamage.breaks(RULES, 3, 2));
        assertFalse(ShipDamage.breaks(RULES, 5.9, 4));
        assertFalse(ShipDamage.breaks(RULES, 1000, -1));
    }
}
