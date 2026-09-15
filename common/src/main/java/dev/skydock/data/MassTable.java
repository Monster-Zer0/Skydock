package dev.skydock.data;

import com.google.gson.*;
import dev.skydock.Skydock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Portable datapack mass rules. Loader-specific data maps never leak into ship physics. */
public final class MassTable extends SimpleJsonResourceReloadListener {
    private static Map<ResourceLocation, Double> masses = Map.of();
    private static double defaultMass = 10, liftPerCell = 1000;
    private static int blocksPerCell = 128;
    private static Map<String, Integer> caps = Map.of();
    public MassTable() { super(new Gson(), "skydock_mass"); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Double> next = new HashMap<>();
        Map<String, Integer> nextCaps = new HashMap<>();
        double nextDefault = 10, nextLift = 1000; int nextSlice = 128;
        for (var file : new TreeMap<>(files).entrySet()) {
            try {
                JsonObject json = file.getValue().getAsJsonObject();
                // Validate a whole file before committing any of its overrides.
                Map<ResourceLocation, Double> fileMasses = new HashMap<>();
                Map<String, Integer> fileCaps = new HashMap<>();
                double fileDefault = json.has("default_mass") ? positive(json.get("default_mass").getAsDouble()) : nextDefault;
                double fileLift = json.has("lift_per_cell") ? positive(json.get("lift_per_cell").getAsDouble()) : nextLift;
                int fileSlice = json.has("blocks_per_cell") ? (int) Math.min(24576, positive(json.get("blocks_per_cell").getAsInt())) : nextSlice;
                if (json.has("blocks")) for (var e : json.getAsJsonObject("blocks").entrySet()) {
                    ResourceLocation id = ResourceLocation.parse(e.getKey());
                    if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Unknown block " + id);
                    fileMasses.put(id, positive(e.getValue().getAsDouble()));
                }
                if (json.has("dock_caps")) for (DockTier tier : DockTier.values()) {
                    if (json.getAsJsonObject("dock_caps").has(tier.key())) fileCaps.put(tier.key(),
                            (int) Math.min(24576, positive(json.getAsJsonObject("dock_caps").get(tier.key()).getAsInt())));
                }
                next.putAll(fileMasses); nextCaps.putAll(fileCaps);
                nextDefault = fileDefault; nextLift = fileLift; nextSlice = fileSlice;
            } catch (RuntimeException ex) { Skydock.LOGGER.error("Invalid Skydock mass rules {}", file.getKey(), ex); }
        }
        masses = Map.copyOf(next); caps = Map.copyOf(nextCaps);
        defaultMass = nextDefault; liftPerCell = nextLift; blocksPerCell = nextSlice;
    }
    private static double positive(double value) {
        if (!Double.isFinite(value) || value <= 0 || value > 1e9) throw new IllegalArgumentException("Value must be finite, positive, and at most 1,000,000,000");
        return value;
    }
    /** Decorations still count toward dock caps, but never add mass or structural lift demand. */
    public static double mass(BlockState state) {
        if (state.is(dev.skydock.block.SkydockBlocks.DECORATIONS)) return 0;
        return masses.getOrDefault(BuiltInRegistries.BLOCK.getKey(state.getBlock()), defaultMass);
    }
    public static double liftPerCell() { return liftPerCell; }
    public static int blocksPerCell() { return blocksPerCell; }
    public static int cap(DockTier tier) { return caps.getOrDefault(tier.key(), tier.defaultCap); }
}
