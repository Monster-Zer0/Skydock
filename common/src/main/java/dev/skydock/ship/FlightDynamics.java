package dev.skydock.ship;

public final class FlightDynamics {
    // Baseline (unpowered) and best-tier speed limits, 20% above the original .35 / .52 envelope.
    public static final double MAX_SPEED = .42;
    public static final double MAX_TIER_SPEED = .624;
    public static final double ACCELERATION = .012;
    private FlightDynamics() {}
    public static double approach(double current, double target, double step) {
        return current + Math.clamp(target - current, -step, step);
    }
    public static double turnRate(double mass, double hullLength) {
        return Math.clamp(1.8 / Math.max(1, Math.sqrt(mass / 20000) * Math.max(1, hullLength / 48)), .45, 1.8);
    }
    public static double cruiseThrottle(double speed, double target) {
        return Math.clamp((.03 * target + .2 * (target - speed)) / ACCELERATION, -1, 1);
    }
}
