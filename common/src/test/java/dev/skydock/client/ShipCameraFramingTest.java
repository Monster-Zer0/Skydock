package dev.skydock.client;

import org.junit.jupiter.api.Test;

import static dev.skydock.client.ShipCameraFraming.*;
import static org.junit.jupiter.api.Assertions.*;

class ShipCameraFramingTest {
    @Test void largerHullsFrameFartherAway() {
        double scout = fitDistance(11, 14, 24, 70), dreadnought = fitDistance(40, 30, 80, 70);
        assertTrue(scout > PLAYER_DISTANCE + 2 && scout < 40, "scout " + scout);
        assertTrue(dreadnought > 2.5 * scout, "dreadnought " + dreadnought);
    }

    @Test void tinyHullsStillPullBackPastThePlayerView() {
        assertEquals(PLAYER_DISTANCE + 2, fitDistance(1, 1, 1, 70), 1e-9);
    }

    @Test void widerFieldOfViewFramesCloser() {
        assertTrue(fitDistance(11, 14, 24, 110) < fitDistance(11, 14, 24, 70));
        assertEquals(fitDistance(11, 14, 24, 110), fitDistance(11, 14, 24, 170), 1e-9);
    }

    @Test void maximumZoomStaysInsideRenderedTerrain() {
        assertEquals(64, maxDistance(200, 8), 1e-9);
        assertEquals(24, maxDistance(200, 2), 1e-9);
        assertEquals(50, maxDistance(20, 32), 1e-9);
        assertEquals(PLAYER_DISTANCE + 2, maxDistance(1, 32), 1e-9);
    }

    @Test void scrollNotchesAreSymmetricAndClamped() {
        assertTrue(zoom(1, 1) < 1);
        assertTrue(zoom(1, -1) > 1);
        assertEquals(1, zoom(zoom(1, 3), -3), 1e-12);
        assertEquals(MIN_DISTANCE / 20, clampZoom(0.001, 20, 50), 1e-12);
        assertEquals(50 / 20.0, clampZoom(99, 20, 50), 1e-12);
        assertEquals(1, clampZoom(1, 20, 50), 1e-12);
    }

    @Test void focusMovesFromPlayerToHullAcrossTheZoomRange() {
        assertEquals(0, focusBlend(MIN_DISTANCE, 20), 1e-12);
        assertEquals(0, focusBlend(PLAYER_DISTANCE, 20), 1e-12);
        assertEquals(.5, focusBlend(12, 20), 1e-12);
        assertEquals(1, focusBlend(20, 20), 1e-12);
        assertEquals(1, focusBlend(60, 20), 1e-12);
        double previous = 0;
        for (double d = PLAYER_DISTANCE; d <= 20; d += .5) { double b = focusBlend(d, 20); assertTrue(b >= previous); previous = b; }
    }

    @Test void easingIsFrameRateIndependent() {
        assertEquals(4, ease(4, 30, 0, 10), 1e-12);
        assertEquals(30, ease(4, 30, 10, 10), 1e-9);
        assertEquals(ease(4, 30, 1 / 30.0, 10), ease(ease(4, 30, 1 / 60.0, 10), 30, 1 / 60.0, 10), 1e-12);
    }
}
