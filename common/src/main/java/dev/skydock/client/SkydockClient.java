package dev.skydock.client;

import dev.architectury.event.events.client.*;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.skydock.block.SkydockBlocks;
import dev.skydock.network.DockNetwork;
import dev.skydock.network.ShipNetwork;
import dev.skydock.ship.*;
import net.minecraft.client.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class SkydockClient {
    public static final Map<UUID, Ship> SHIPS = new LinkedHashMap<>();
    private static final Map<UUID, Ship> pending = new HashMap<>();
    private static final Map<UUID, ShipMotion> motions = new HashMap<>();
    private static final KeyMapping RELEASE = new KeyMapping("key.skydock.release", 82, "key.categories.skydock");
    private static final KeyMapping CRUISE = new KeyMapping("key.skydock.cruise", 67, "key.categories.skydock");
    private static boolean inWorld;
    private static long tick;
    private static float carriedYaw;
    public static void init() {
        dev.architectury.registry.ReloadListenerRegistry.register(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> Minecraft.getInstance().execute(ShipRenderer::clear), dev.skydock.Skydock.id("hull_meshes"));
        ShipNetwork.clientReceiver = SkydockClient::receive;
        DockNetwork.clientReceiver = DockView::receive;
        ShipManager.ClientShipAccess.provider = level -> SHIPS.values().stream().filter(s -> s.dimension.equals(level.dimension())).toList();
        SkydockBlocks.DOCK_MENU.listen(type -> MenuRegistry.registerScreenFactory(type, DockScreen::new));
        SkydockBlocks.DEVICE_MENU.listen(type -> MenuRegistry.registerScreenFactory(type, DeviceScreen::new));
        SkydockBlocks.ENGINE_MENU.listen(type -> MenuRegistry.registerScreenFactory(type, EngineScreen::new));
        KeyMappingRegistry.register(RELEASE);
        KeyMappingRegistry.register(CRUISE);
        ClientTickEvent.CLIENT_PRE.register(SkydockClient::tick);
        ClientTickEvent.CLIENT_POST.register(SkydockClient::moveShips);
        ClientGuiEvent.RENDER_HUD.register((graphics, delta) -> {
            Ship ship = piloted();
            if (ship == null) ship = aboard();
            if (ship == null) return;
            var mc = Minecraft.getInstance();
            String text = String.format(Locale.ROOT, "SKYDOCK  |  %.1f blocks/s  |  Altitude %.0f%s%s",
                    ship.velocity.length() * 20, ship.pose.y(), ship.cruise ? String.format(Locale.ROOT, "  |  CRUISE %.1f blocks/s", ship.cruiseSpeed * 20) : "", ship.blocked ? "  |  OBSTRUCTED" : "");
            String controls = piloted() != null
                    ? thrustKeys() + " speed   " + turnKeys() + " turn   " + altitudeKeys() + " altitude   " + cruiseKey() + " cruise   " + releaseKey() + " release"
                    : "Walk and use storage aboard   " + cruiseKey() + " cruise";
            graphics.fill(8, 8, Math.max(mc.font.width(text), mc.font.width(controls)) + 20, 40, 0xCC101E28);
            graphics.drawString(mc.font, text, 14, 14, 0xFF9AE4D2);
            graphics.drawString(mc.font, controls, 14, 27, 0xFFE3E6E8);
        });
    }
    /** Resolved name of a binding, so help text follows the player's own controls instead of the defaults. */
    public static String keyLabel(KeyMapping mapping) { return mapping.getTranslatedKeyMessage().getString(); }
    public static String thrustKeys() { var options = Minecraft.getInstance().options; return keyLabel(options.keyUp) + "/" + keyLabel(options.keyDown); }
    public static String turnKeys() { var options = Minecraft.getInstance().options; return keyLabel(options.keyLeft) + "/" + keyLabel(options.keyRight); }
    public static String altitudeKeys() { var options = Minecraft.getInstance().options; return keyLabel(options.keyJump) + "/" + keyLabel(options.keyShift); }
    public static String cruiseKey() { return keyLabel(CRUISE); }
    public static String releaseKey() { return keyLabel(RELEASE); }
    public static Ship piloted() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        return SHIPS.values().stream().filter(s -> player.getUUID().equals(s.pilot)).findFirst().orElse(null);
    }
    public static boolean seated() {
        var player = Minecraft.getInstance().player;
        return player != null && SHIPS.values().stream().anyMatch(s -> s.seated.containsKey(player.getUUID()));
    }
    public static float carriedYaw() { return carriedYaw; }
    public static ShipMovementFrame.Frame movementFrame(LocalPlayer player) {
        Ship ship = ShipManager.attachedShip(player);
        if (ship == null) {
            ship = SHIPS.values().stream().filter(candidate -> candidate.dimension.equals(player.level().dimension())
                    && ShipCollision.supported(candidate, candidate.pose, player.getBoundingBox())).findFirst().orElse(null);
            if (ship != null) ShipManager.attach(player, ship);
        }
        ShipMotion motion = ship == null ? null : motions.get(ship.id);
        return motion == null ? ShipMovementFrame.Frame.none(0) : ShipManager.movementFrame(player, motion.time());
    }
    public static boolean useViewedShip() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        ShipInteractions.Hit hit = ShipInteractions.pick(player);
        if (hit == null) return false;
        Ship ship = hit.ship(); ShipMotion motion = motions.get(ship.id);
        if (motion != null) ShipNetwork.use(ship.id, motion.time(), ship.worldToBlock(player.getEyePosition()),
                ShipPose.rotate(player.getLookAngle().scale(player.blockInteractionRange()), -ship.pose.yaw()));
        return true;
    }
    private static Ship aboard() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        return SHIPS.values().stream().filter(s -> s.dimension.equals(player.level().dimension()) && ShipManager.permitted(player, s)
                && s.pose.toWorld(s.hullBounds()).inflate(1).contains(player.position())).findFirst().orElse(null);
    }
    private static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            if (inWorld) { SHIPS.clear(); pending.clear(); motions.clear(); ShipRenderer.clear(); }
            carriedYaw = 0;
            inWorld = false; return;
        }
        if (!inWorld) { ShipNetwork.input(3, 0, 0, 0); inWorld = true; }
        tick++;
        if (mc.screen == null && RELEASE.consumeClick()) ShipNetwork.input(2, 0, 0, 0);
        if (mc.screen == null && CRUISE.consumeClick()) ShipNetwork.input(4, 0, 0, 0);
        Ship pilot = piloted();
        if (pilot != null && tick % 2 == 0) {
            float thrust = mc.screen == null ? axis(mc.options.keyUp.isDown(), mc.options.keyDown.isDown()) : 0;
            float turn = mc.screen == null ? axis(mc.options.keyRight.isDown(), mc.options.keyLeft.isDown()) : 0;
            float climb = mc.screen == null ? axis(mc.options.keyJump.isDown(), mc.options.keyShift.isDown()) : 0;
            ShipNetwork.input(0, thrust, turn, climb);
        }
        if (tick % 20 == 0) ShipRenderer.findDocks(mc);
    }
    private static void moveShips(Minecraft mc) {
        carriedYaw = 0;
        if (mc.level == null || mc.player == null) return;
        float yawBeforeCarry = mc.player.getYRot();
        // Vanilla has now recorded entity old positions. Deck and rider share the same render interval.
        for (Ship ship : SHIPS.values()) {
            ship.previousPose = ship.pose;
            ShipMotion motion = motions.get(ship.id);
            if (motion != null) {
                ship.pose = motion.advance();
                if (ship.dimension.equals(mc.level.dimension())) ShipManager.carry(mc.level, ship, ship.previousPose);
            }
        }
        carriedYaw = net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot() - yawBeforeCarry);
    }
    private static float axis(boolean positive, boolean negative) { return (positive ? 1 : 0) - (negative ? 1 : 0); }
    private static void receive(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null) return;
        if (data.getString("Kind").equals("deck_correction")) {
            Ship ship = SHIPS.get(data.getUUID("Id"));
            if (ship != null && mc.player != null) {
                Vec3 local = new Vec3(data.getDouble("LocalX"), data.getDouble("LocalY"), data.getDouble("LocalZ"));
                mc.player.setPos(ship.blockToWorld(local));
                ShipManager.attach(mc.player, ship);
                mc.player.setOldPosAndRot();
            }
            return;
        }
        if (data.getString("Kind").equals("manifest")) {
            Set<UUID> active = new HashSet<>(); for (Tag e : data.getList("Ships", Tag.TAG_COMPOUND)) active.add(((CompoundTag) e).getUUID("Id"));
            SHIPS.keySet().retainAll(active); pending.keySet().retainAll(active); motions.keySet().retainAll(active); ShipRenderer.retain(active); return;
        }
        UUID id = data.getUUID("Id");
        if (data.getString("Kind").equals("blocks")) {
            if (data.getInt("Part") == 0) pending.put(id, Ship.load(data, mc.level.registryAccess()));
            Ship ship = pending.get(id); if (ship == null) return;
            for (Tag e : data.getList("CellsData", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) e; BlockPos pos = BlockPos.of(entry.getLong("Pos"));
                ship.blocks.put(pos, Block.stateById(entry.getInt("State")));
                if (entry.contains("Data")) ship.blockEntities.put(pos, entry.getCompound("Data"));
            }
            if (data.getInt("Part") + 1 == data.getInt("Parts")) {
                pending.remove(id);
                ShipMotion motion = motions.computeIfAbsent(id, key -> new ShipMotion());
                motion.accept(data.getLong("Tick"), ship.pose, ship.velocity, ship.yawVelocity);
                Ship old = SHIPS.put(id, ship);
                if (old != null) { ship.pose = old.pose; ship.previousPose = old.previousPose; ship.seated.putAll(old.seated); }
                ShipRenderer.invalidate(id);
                if (data.getBoolean("Boarding") && mc.player != null) {
                    mc.player.setPos(ship.blockToWorld(new net.minecraft.world.phys.Vec3(data.getDouble("BoardX"), data.getDouble("BoardY"), data.getDouble("BoardZ"))));
                    mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); mc.player.setOnGround(true); mc.player.fallDistance = 0;
                    ShipManager.attach(mc.player, ship);
                    mc.player.setOldPosAndRot();
                    ShipNetwork.boardReady(id);
                }
            }
        } else if (data.getString("Kind").equals("pose")) {
            Ship ship = SHIPS.get(id); if (ship == null) return;
            ship.velocity = new net.minecraft.world.phys.Vec3(data.getDouble("Vx"), data.getDouble("Vy"), data.getDouble("Vz"));
            ship.yawVelocity = data.getDouble("YawVelocity"); ship.cruise = data.getBoolean("Cruise"); ship.cruiseSpeed = data.getDouble("CruiseSpeed");
            motions.computeIfAbsent(id, key -> new ShipMotion()).accept(data.getLong("Tick"),
                    new ShipPose(data.getDouble("X"), data.getDouble("Y"), data.getDouble("Z"), data.getDouble("Yaw")), ship.velocity, ship.yawVelocity);
            ship.pilot = data.hasUUID("Pilot") ? data.getUUID("Pilot") : null;
            ship.pilotAnchor = data.contains("AnchorX") ? new net.minecraft.world.phys.Vec3(data.getDouble("AnchorX"), data.getDouble("AnchorY"), data.getDouble("AnchorZ")) : null;
            ship.lift = data.getDouble("Lift"); ship.mass = data.getDouble("Mass"); ship.blocked = data.getBoolean("Blocked");
            ship.seated.clear(); for (Tag e : data.getList("Seated", Tag.TAG_COMPOUND)) {
                CompoundTag tag = (CompoundTag) e; ship.seated.put(tag.getUUID("Player"), BlockPos.of(tag.getLong("Pos")));
            }
        }
    }
}
