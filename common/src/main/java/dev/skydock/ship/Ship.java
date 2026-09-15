package dev.skydock.ship;

import dev.skydock.data.DockTier;
import dev.skydock.block.FenceConnections;
import dev.skydock.block.SkydockBlocks;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.*;

public final class Ship {
    public UUID id = UUID.randomUUID(), owner;
    public String team = "";
    public DockTier tier;
    public ResourceKey<Level> dimension = Level.OVERWORLD;
    public BlockPos dock, yard;
    public ShipPose pose, previousPose;
    public Vec3 velocity = Vec3.ZERO;
    public final ShipMotion history = new ShipMotion();
    public final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    public final Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
    public double mass, lift;
    public int cells, engines, revision;
    public UUID pilot;
    public BlockPos pilotHelm;
    public float throttle, steering, climb;
    public double appliedThrottle, appliedClimb, yawVelocity;
    public boolean cruise;
    public double cruiseSpeed;
    public Vec3 pilotAnchor;
    private Vec3 pivot;
    private AABB cachedHullBounds;
    private int hullRevision = -1;
    public long lastInputTick;
    public boolean moored = true, blocked;
    public String phase = "active";
    public ResourceKey<Level> transferDimension;
    public BlockPos transferOrigin, transferDock;
    public final Map<UUID, BlockPos> seated = new HashMap<>();

    public Vec3 center() { return pivot == null ? legacyCenter() : pivot; }
    private Vec3 legacyCenter() { return new Vec3(tier.width / 2.0, 0, tier.length / 2.0); }
    void initializePivot() {
        if (pivot != null) return;
        Vec3 old = legacyCenter(), next = occupiedCenter();
        pivot = next; cachedHullBounds = null; hullRevision = -1;
        if (pose != null) pose = rebase(pose, old, next);
        if (previousPose != null) previousPose = rebase(previousPose, old, next);
    }
    private Vec3 occupiedCenter() {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (var entry : blocks.entrySet()) if (!entry.getValue().isAir() && !entry.getValue().is(SkydockBlocks.DECORATIONS)) {
            minX = Math.min(minX, entry.getKey().getX()); maxX = Math.max(maxX, entry.getKey().getX());
            minZ = Math.min(minZ, entry.getKey().getZ()); maxZ = Math.max(maxZ, entry.getKey().getZ());
        }
        return minX == Integer.MAX_VALUE ? legacyCenter() : new Vec3((minX + maxX + 1) / 2.0, 0, (minZ + maxZ + 1) / 2.0);
    }
    static ShipPose rebase(ShipPose pose, Vec3 oldCenter, Vec3 newCenter) {
        Vec3 position = ShipPose.rotate(newCenter.subtract(oldCenter), pose.yaw()).add(pose.x(), pose.y(), pose.z());
        return new ShipPose(position.x, pose.y(), position.z, pose.yaw());
    }
    public Vec3 blockToWorld(Vec3 block) { return pose.toWorld(block.subtract(center())); }
    public Vec3 worldToBlock(Vec3 world) { return pose.toLocal(world).add(center()); }
    public AABB localBounds() { return new AABB(0, 0, 0, tier.width, tier.height, tier.length).move(center().scale(-1)); }
    public AABB bounds() { return pose.toWorld(localBounds()); }
    public AABB hullBounds() {
        if (cachedHullBounds == null || hullRevision != revision) {
            AABB box = null;
            for (var entry : blocks.entrySet()) if (!entry.getValue().isAir()) {
                AABB cell = new AABB(entry.getKey()).move(center().scale(-1));
                box = box == null ? cell : box.minmax(cell);
            }
            cachedHullBounds = box == null ? localBounds() : box;
            hullRevision = revision;
        }
        return cachedHullBounds;
    }
    public BlockState state(BlockPos p) { return blocks.getOrDefault(p, Blocks.AIR.defaultBlockState()); }

    boolean refreshRailingConnections() {
        var railing = SkydockBlocks.TIMBER_RAILING.getOrNull();
        if (railing == null) return false;
        boolean changed = false;
        Map<BlockPos, BlockState> resolved = new LinkedHashMap<>();
        for (var entry : blocks.entrySet()) {
            BlockState state = entry.getValue();
            if (state.is(railing))
                state = FenceConnections.resolve(state, entry.getKey(), this::state, net.minecraft.world.level.EmptyBlockGetter.INSTANCE);
            resolved.put(entry.getKey(), state);
            changed |= state != entry.getValue();
        }
        if (changed) {
            blocks.clear();
            blocks.putAll(resolved);
            revision++;
        }
        return changed;
    }

    public CompoundTag save(HolderLookup.Provider registries, boolean includeBlocks) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id); if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("Team", team); tag.putString("Tier", tier.name()); tag.putString("Dimension", dimension.location().toString());
        tag.putLong("Dock", dock.asLong()); tag.putLong("Yard", yard.asLong());
        tag.putDouble("X", pose.x()); tag.putDouble("Y", pose.y()); tag.putDouble("Z", pose.z()); tag.putDouble("Yaw", pose.yaw());
        Vec3 center = center(); tag.putDouble("PivotX", center.x); tag.putDouble("PivotZ", center.z);
        tag.putDouble("Vx", velocity.x); tag.putDouble("Vy", velocity.y); tag.putDouble("Vz", velocity.z);
        tag.putDouble("YawVelocity", yawVelocity); tag.putBoolean("Cruise", cruise); tag.putDouble("CruiseSpeed", cruiseSpeed);
        tag.putDouble("Mass", mass); tag.putDouble("Lift", lift); tag.putBoolean("Moored", moored);
        tag.putBoolean("Blocked", blocked); tag.putInt("Revision", revision); tag.putInt("Cells", cells); tag.putInt("Engines", engines);
        tag.putString("Phase", phase);
        if (transferOrigin != null) {
            tag.putLong("TransferOrigin", transferOrigin.asLong()); tag.putLong("TransferDock", transferDock.asLong());
            tag.putString("TransferDimension", transferDimension.location().toString());
        }
        if (pilot != null) tag.putUUID("Pilot", pilot);
        if (pilot != null && pilotAnchor != null) {
            tag.putDouble("AnchorX", pilotAnchor.x); tag.putDouble("AnchorY", pilotAnchor.y); tag.putDouble("AnchorZ", pilotAnchor.z);
        }
        if (includeBlocks) {
            ListTag list = new ListTag();
            blocks.forEach((p, state) -> {
                CompoundTag e = new CompoundTag(); e.putLong("Pos", p.asLong()); e.put("State", NbtUtils.writeBlockState(state));
                if (blockEntities.containsKey(p)) e.put("Data", blockEntities.get(p).copy());
                list.add(e);
            }); tag.put("Blocks", list);
        }
        return tag;
    }

    public static Ship load(CompoundTag tag, HolderLookup.Provider registries) { return load(tag, registries, false); }
    static Ship loadPersisted(CompoundTag tag, HolderLookup.Provider registries) { return load(tag, registries, true); }
    private static Ship load(CompoundTag tag, HolderLookup.Provider registries, boolean migrateLegacy) {
        Ship ship = new Ship();
        ship.id = tag.getUUID("Id"); ship.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ship.team = tag.getString("Team"); ship.tier = DockTier.valueOf(tag.getString("Tier"));
        ship.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")));
        ship.dock = BlockPos.of(tag.getLong("Dock")); ship.yard = BlockPos.of(tag.getLong("Yard"));
        ship.pose = new ShipPose(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"), tag.getDouble("Yaw")); ship.previousPose = ship.pose;
        if (tag.contains("PivotX", Tag.TAG_DOUBLE) && tag.contains("PivotZ", Tag.TAG_DOUBLE))
            ship.pivot = new Vec3(tag.getDouble("PivotX"), 0, tag.getDouble("PivotZ"));
        ship.velocity = new Vec3(tag.getDouble("Vx"), tag.getDouble("Vy"), tag.getDouble("Vz"));
        ship.yawVelocity = tag.getDouble("YawVelocity"); ship.cruise = tag.getBoolean("Cruise");
        ship.cruiseSpeed = Math.clamp(tag.getDouble("CruiseSpeed"), -FlightDynamics.MAX_TIER_SPEED, FlightDynamics.MAX_TIER_SPEED);
        ship.mass = tag.getDouble("Mass"); ship.lift = tag.getDouble("Lift"); ship.moored = tag.getBoolean("Moored");
        ship.blocked = tag.getBoolean("Blocked"); ship.revision = tag.getInt("Revision"); ship.cells = tag.getInt("Cells"); ship.engines = tag.getInt("Engines");
        ship.phase = tag.getString("Phase");
        if (tag.contains("TransferOrigin")) {
            ship.transferOrigin = BlockPos.of(tag.getLong("TransferOrigin")); ship.transferDock = BlockPos.of(tag.getLong("TransferDock"));
            ship.transferDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("TransferDimension")));
        }
        if (tag.hasUUID("Pilot")) ship.pilot = tag.getUUID("Pilot");
        if (tag.contains("AnchorX")) ship.pilotAnchor = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
        var lookup = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag e : tag.getList("Blocks", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) e; BlockPos p = BlockPos.of(entry.getLong("Pos"));
            ship.blocks.put(p, NbtUtils.readBlockState(lookup, entry.getCompound("State")));
            if (entry.contains("Data")) ship.blockEntities.put(p, entry.getCompound("Data"));
        }
        if (migrateLegacy) ship.refreshRailingConnections();
        if (migrateLegacy && ship.pivot == null) ship.initializePivot();
        return ship;
    }
}
