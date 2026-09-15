package dev.skydock.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.skydock.block.*;
import dev.skydock.ship.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import java.util.*;

public final class ShipRenderer {
    private record Mesh(List<Layer> layers, List<BlockPos> dynamic, Map<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> entities) implements AutoCloseable {
        @Override public void close() { layers.forEach(layer -> layer.buffer.close()); }
    }
    private record Layer(RenderType type, VertexBuffer buffer) {}
    private static final Map<UUID, Mesh> meshes = new HashMap<>();
    private static final Map<BlockPos, dev.skydock.data.DockTier> docks = new HashMap<>();
    public static void clear() { meshes.values().forEach(Mesh::close); meshes.clear(); docks.clear(); }
    public static void invalidate(UUID id) { Mesh mesh = meshes.remove(id); if (mesh != null) mesh.close(); }
    public static void retain(Set<UUID> ids) { for (UUID id : Set.copyOf(meshes.keySet())) if (!ids.contains(id)) invalidate(id); }
    public static void findDocks(Minecraft mc) {
        docks.clear();
        int cx = mc.player.chunkPosition().x, cz = mc.player.chunkPosition().z;
        for (int x = cx - 6; x <= cx + 6; x++) for (int z = cz - 6; z <= cz + 6; z++) {
            var chunk = mc.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk != null) chunk.getBlockEntities().forEach((p, entity) -> {
                if (entity instanceof DockBlockEntity dock) docks.put(p, dock.tier());
            });
        }
    }
    private static Mesh bake(Ship ship) {
        Minecraft mc = Minecraft.getInstance(); ShipRenderView view = new ShipRenderView(ship);
        Map<RenderType, List<BlockPos>> byType = new LinkedHashMap<>(); List<BlockPos> dynamic = new ArrayList<>();
        for (var entry : ship.blocks.entrySet()) {
            BlockState state = entry.getValue(); if (state.isAir()) continue;
            RenderType layer = ItemBlockRenderTypes.getChunkRenderType(state);
            if (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED || layer == RenderType.translucent()) dynamic.add(entry.getKey());
            else if (state.getRenderShape() == RenderShape.MODEL) byType.computeIfAbsent(layer, key -> new ArrayList<>()).add(entry.getKey());
        }
        List<Layer> layers = new ArrayList<>();
        for (var entry : byType.entrySet()) {
            try (ByteBufferBuilder bytes = new ByteBufferBuilder(2 * 1024 * 1024)) {
                BufferBuilder buffer = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                PoseStack poses = new PoseStack();
                for (BlockPos p : entry.getValue()) {
                    BlockState state = ship.state(p); poses.pushPose(); poses.translate(p.getX() - ship.center().x, p.getY(), p.getZ() - ship.center().z);
                    mc.getBlockRenderer().getModelRenderer().tesselateBlock(view, mc.getBlockRenderer().getBlockModel(state), state, p, poses, buffer, true, RandomSource.create(42), state.getSeed(p), OverlayTexture.NO_OVERLAY);
                    poses.popPose();
                }
                MeshData data = buffer.build();
                if (data != null) { VertexBuffer vertex = new VertexBuffer(VertexBuffer.Usage.STATIC); vertex.bind(); vertex.upload(data); VertexBuffer.unbind(); layers.add(new Layer(entry.getKey(), vertex)); }
            }
        }
        Map<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> entities = new HashMap<>();
        for (BlockPos pos : ship.blocks.keySet()) if (ship.state(pos).getBlock() instanceof net.minecraft.world.level.block.EntityBlock block) {
            var be = block.newBlockEntity(pos, ship.state(pos));
            if (be != null) {
                be.setLevel(mc.level);
                if (ship.blockEntities.containsKey(pos)) be.loadWithComponents(ship.blockEntities.get(pos), mc.level.registryAccess());
                entities.put(pos, be);
                if (!dynamic.contains(pos)) dynamic.add(pos);
            }
        }
        return new Mesh(layers, dynamic, entities);
    }
    public static void render(Matrix4f viewMatrix, Matrix4f projection, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null || mc.player == null) return;
        Vec3 eye = camera.getPosition();
        for (Ship ship : SkydockClient.SHIPS.values()) {
            if (!ship.dimension.equals(mc.level.dimension()) || ship.bounds().distanceToSqr(eye) > 256 * 256) continue;
            Mesh mesh = meshes.computeIfAbsent(ship.id, id -> bake(ship));
            ShipPose a = ship.previousPose, b = ship.pose;
            double x = a.x() + (b.x() - a.x()) * partialTick, y = a.y() + (b.y() - a.y()) * partialTick, z = a.z() + (b.z() - a.z()) * partialTick;
            float yaw = (float) (a.yaw() + net.minecraft.util.Mth.wrapDegrees(b.yaw() - a.yaw()) * partialTick);
            Matrix4f model = new Matrix4f(viewMatrix).translate((float) (x - eye.x), (float) (y - eye.y), (float) (z - eye.z)).rotateY((float) Math.toRadians(-yaw));
            for (Layer layer : mesh.layers) {
                layer.type.setupRenderState();
                var shader = RenderSystem.getShader(); if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(0.0f, 0.0f, 0.0f);
                layer.buffer.bind(); layer.buffer.drawWithShader(model, projection, shader); VertexBuffer.unbind(); layer.type.clearRenderState();
            }
            // Buffered entity layers already receive the camera view matrix from Minecraft.
            PoseStack poses = new PoseStack(); poses.translate(x - eye.x, y - eye.y, z - eye.z); poses.mulPose(Axis.YP.rotationDegrees(-yaw));
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            for (BlockPos p : mesh.dynamic) {
                poses.pushPose(); poses.translate(p.getX() - ship.center().x, p.getY(), p.getZ() - ship.center().z);
                int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(ship.blockToWorld(Vec3.atCenterOf(p))));
                var be = mesh.entities.get(p);
                if (be != null) mc.getBlockEntityRenderDispatcher().renderItem(be, poses, buffers, light, OverlayTexture.NO_OVERLAY);
                if (be == null && (ship.state(p).getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED || ItemBlockRenderTypes.getChunkRenderType(ship.state(p)) == RenderType.translucent()))
                    mc.getBlockRenderer().renderSingleBlock(ship.state(p), poses, buffers, light, OverlayTexture.NO_OVERLAY);
                poses.popPose();
            }
            buffers.endBatch();
        }
        PoseStack outlines = new PoseStack(); outlines.translate(-eye.x, -eye.y, -eye.z);
        var buffers = mc.renderBuffers().bufferSource();
        var nearest = docks.entrySet().stream().min(Comparator.comparingDouble(e -> e.getKey().distToCenterSqr(mc.player.position()))).orElse(null);
        if (nearest != null) LevelRenderer.renderLineBox(outlines, buffers.getBuffer(RenderType.lines()), nearest.getValue().envelope(nearest.getKey()), .35f, .9f, .76f, .8f);
        if (mc.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof DockControllerBlock dock && mc.hitResult instanceof BlockHitResult hit) {
            LevelRenderer.renderLineBox(outlines, buffers.getBuffer(RenderType.lines()), dock.tier.envelope(hit.getBlockPos().relative(hit.getDirection())), .5f, .75f, 1, .8f);
        }
        ShipInteractions.Hit hit = ShipInteractions.pick(mc.player);
        if (hit != null) {
            Ship ship = hit.ship(); ShipPose pose = ship.previousPose.interpolate(ship.pose, partialTick); PoseStack selection = new PoseStack();
            selection.translate(pose.x() - eye.x, pose.y() - eye.y, pose.z() - eye.z); selection.mulPose(Axis.YP.rotationDegrees((float) -pose.yaw()));
            selection.translate(-ship.center().x, 0, -ship.center().z);
            LevelRenderer.renderLineBox(selection, buffers.getBuffer(RenderType.lines()), new AABB(hit.localHit().getBlockPos()).inflate(.003), .9f, 1, 1, .9f);
        }
        buffers.endBatch(RenderType.lines());
    }
}
