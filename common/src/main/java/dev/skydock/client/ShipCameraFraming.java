package dev.skydock.client;

/** Ship camera distances in blocks. Zoom is a multiple of the hull's fit distance, so it carries between ship sizes. */
public final class ShipCameraFraming {
    /** Vanilla's detached distance. At or inside it the camera orbits the player's eyes instead of the hull. */
    public static final double PLAYER_DISTANCE = 4;
    public static final double MIN_DISTANCE = 2;
    public static final double ZOOM_STEP = 1.15;
    /** Below 1 the hull's bounding sphere slightly overfills the view; real hulls are narrower than that sphere. */
    private static final double FRAMING = .8;
    private ShipCameraFraming() {}

    public static double fitDistance(double width, double height, double length, double fovDegrees) {
        double radius = Math.sqrt(width * width + height * height + length * length) / 2;
        double halfFov = Math.toRadians(Math.clamp(fovDegrees, 30, 110)) / 2;
        return Math.max(PLAYER_DISTANCE + 2, radius / Math.sin(halfFov) * FRAMING);
    }
    /** Chunk rendering follows the camera, so stay well inside the terrain loaded around the player. */
    public static double maxDistance(double fit, int renderDistanceChunks) {
        double terrain = Math.max(24, renderDistanceChunks * 16 * .5);
        return Math.max(PLAYER_DISTANCE + 2, Math.min(fit * 2.5, terrain));
    }
    public static double clampZoom(double zoom, double fit, double max) { return Math.clamp(zoom, MIN_DISTANCE / fit, max / fit); }
    /** Positive notches (wheel away from the player) zoom in. */
    public static double zoom(double zoom, double notches) { return zoom * Math.pow(ZOOM_STEP, -notches); }
    /** 0 orbits the player's eyes and 1 the hull; smoothstep keeps the focus from lurching at either end. */
    public static double focusBlend(double distance, double fit) {
        if (fit <= PLAYER_DISTANCE) return 1;
        double t = Math.clamp((distance - PLAYER_DISTANCE) / (fit - PLAYER_DISTANCE), 0, 1);
        return t * t * (3 - 2 * t);
    }
    /** Frame-rate independent exponential approach. */
    public static double ease(double current, double target, double seconds, double rate) {
        return target + (current - target) * Math.exp(-rate * seconds);
    }
}
