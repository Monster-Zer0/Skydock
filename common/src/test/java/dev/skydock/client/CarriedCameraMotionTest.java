package dev.skydock.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarriedCameraMotionTest {
    @Test void carriedTurnMatchesTheHullAtRenderPartialTicks() {
        assertEquals(88.2f, CarriedCameraMotion.interpolateYaw(90, 1.8f, 0), 1e-5f);
        assertEquals(89.1f, CarriedCameraMotion.interpolateYaw(90, 1.8f, .5f), 1e-5f);
        assertEquals(90, CarriedCameraMotion.interpolateYaw(90, 1.8f, 1), 1e-5f);
        assertEquals(91.35f, CarriedCameraMotion.interpolateYaw(90, -1.8f, .25f), 1e-5f);
    }

    @Test void mouseLookRemainsImmediateDuringCarriedTurn() {
        float beforeMouse = CarriedCameraMotion.interpolateYaw(30, 1.8f, .4f);
        float afterMouse = CarriedCameraMotion.interpolateYaw(37, 1.8f, .4f);
        assertEquals(7, afterMouse - beforeMouse, 1e-5f);
    }

    @Test void interpolationStaysContinuousAcrossWrappedShipYaw() {
        assertEquals(180, CarriedCameraMotion.interpolateYaw(181, 2, .5f), 1e-5f);
        assertEquals(-180, CarriedCameraMotion.interpolateYaw(-179, 2, .5f), 1e-5f);
    }
}
