package dev.skydock.client;

public final class CarriedCameraMotion {
    private CarriedCameraMotion() {}

    public static float interpolateYaw(float currentYaw, float carriedTurn, float partialTick) {
        return currentYaw - carriedTurn * (1 - partialTick);
    }
}
