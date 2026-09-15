package dev.skydock.block;

import net.minecraft.world.level.block.state.BlockState;

/** Stable engine performance data shared by physics, menus, and ship patterns. */
public enum EngineTier {
    BRASS(1.0, 1.0, .42),
    COMPOUND(1.5, 1.15, .48),
    TURBINE(2.2, 1.35, .552),
    AETHER(3.2, 1.60, .624);

    public final double thrustMultiplier;
    public final double fuelEfficiency;
    public final double maxSpeed;

    EngineTier(double thrustMultiplier, double fuelEfficiency, double maxSpeed) {
        this.thrustMultiplier = thrustMultiplier;
        this.fuelEfficiency = fuelEfficiency;
        this.maxSpeed = maxSpeed;
    }

    public static EngineTier from(BlockState state) {
        return state.getBlock() instanceof EngineBlock engine ? engine.tier : BRASS;
    }
}
