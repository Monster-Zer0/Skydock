package dev.skydock.data;

import com.google.gson.JsonObject;

/**
 * Collision damage tuning from the {@code collision} object of a mass rules file. Speeds are in blocks per second.
 *
 * @param minSpeed               impacts at or below this speed never cause damage (docking is under 1.6)
 * @param speedPerHardness       excess speed needed per point of block hardness before a hull block breaks
 * @param blocksPerSpeed         hull blocks an impact may break per block/second of excess speed, before the mass factor
 * @param maxBlocks              most hull blocks one impact can break
 * @param entityDamagePerSpeed   health a struck creature loses per block/second of excess speed
 * @param damageOtherShips       whether a ship that is struck is damaged as well as the one that struck it
 */
public record CollisionRules(double minSpeed, double speedPerHardness, double blocksPerSpeed, int maxBlocks,
                             double entityDamagePerSpeed, boolean damageOtherShips) {
    public static final CollisionRules DEFAULT = new CollisionRules(3, 1.5, 1, 32, 1, true);

    /** Reads overrides on top of {@code base}; throws on any invalid value so a bad file changes nothing. */
    static CollisionRules parse(JsonObject json, CollisionRules base) {
        return new CollisionRules(
                json.has("min_speed") ? positive(json.get("min_speed").getAsDouble()) : base.minSpeed,
                json.has("speed_per_hardness") ? positive(json.get("speed_per_hardness").getAsDouble()) : base.speedPerHardness,
                json.has("blocks_per_speed") ? nonNegative(json.get("blocks_per_speed").getAsDouble()) : base.blocksPerSpeed,
                json.has("max_blocks") ? (int) Math.min(4096, nonNegative(json.get("max_blocks").getAsInt())) : base.maxBlocks,
                json.has("entity_damage_per_speed") ? nonNegative(json.get("entity_damage_per_speed").getAsDouble()) : base.entityDamagePerSpeed,
                json.has("damage_other_ships") ? json.get("damage_other_ships").getAsBoolean() : base.damageOtherShips);
    }
    private static double positive(double value) {
        if (!Double.isFinite(value) || value <= 0 || value > 1e6) throw new IllegalArgumentException("Collision values must be finite, positive, and at most 1,000,000");
        return value;
    }
    private static double nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1e6) throw new IllegalArgumentException("Collision values must be finite, zero or more, and at most 1,000,000");
        return value;
    }
}
