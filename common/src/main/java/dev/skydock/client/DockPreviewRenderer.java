package dev.skydock.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

import java.util.*;

/** Screen-owned, isolated miniature mesh. No preview operation modifies the world or a live ship. */
public final class DockPreviewRenderer implements AutoCloseable {
    private record Layer(RenderType type, VertexBuffer buffer) {}
    private record Dynamic(BlockPos pos, BlockState state) {}
    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
    private record Mesh(List<Layer> layers, List<Dynamic> dynamic, Bounds bounds) implements AutoCloseable {
        @Override public void close() { layers.forEach(layer -> layer.buffer.close()); }
    }

    private float yaw = -35.0F;
    private float pitch = 24.0F;
    private float zoom = 1.0F;
    private List<DockView.PreviewBlock> meshSource;
    private Mesh mesh;

    public void reset() {
        yaw = -35.0F;
        pitch = 24.0F;
        zoom = 1.0F;
    }

    public void drag(double dx, double dy) {
        yaw += (float) dx * 0.7F;
        pitch = Mth.clamp(pitch + (float) dy * 0.55F, -75.0F, 75.0F);
    }

    public void zoom(double amount) {
        zoom = Mth.clamp(zoom * (float) Math.pow(1.12, amount), 0.45F, 2.8F);
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, List<DockView.PreviewBlock> blocks) {
        if (width < 8 || height < 8 || blocks.isEmpty()) return;
        // Commit queued GUI layers before mesh upload or direct draws alter buffer and shader state.
        graphics.flush();
        ensureMesh(blocks);
        if (mesh == null) return;
        Bounds bounds = mesh.bounds;
        int minX = bounds.minX, minY = bounds.minY, minZ = bounds.minZ;
        int maxX = bounds.maxX, maxY = bounds.maxY, maxZ = bounds.maxZ;
        float sizeX = maxX - minX + 1.0F, sizeY = maxY - minY + 1.0F, sizeZ = maxZ - minZ + 1.0F;
        float footprint = Math.max(2.0F, sizeX + sizeZ);
        float fit = Math.min(width * 0.82F / footprint, height * 0.84F / Math.max(2.0F, sizeY + footprint * 0.27F));
        float scale = Math.max(0.25F, fit * zoom);
        float centerX = (minX + maxX + 1.0F) * 0.5F;
        float centerY = (minY + maxY + 1.0F) * 0.5F;
        float centerZ = (minZ + maxZ + 1.0F) * 0.5F;

        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        PoseStack poses = graphics.pose();
        poses.pushPose();
        poses.translate(x + width * 0.5F, y + height * 0.54F, 100.0F);
        poses.scale(scale, -scale, scale);
        poses.mulPose(Axis.XP.rotationDegrees(pitch));
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.translate(-centerX, -centerY, -centerZ);
        Lighting.setupForEntityInInventory();
        RenderSystem.enableDepthTest();
        // Buffered GUI render types receive Minecraft's global -11000 Z model-view translation.
        // Direct VBO draws must compose it explicitly or the static mesh is clipped by the GUI projection.
        Matrix4f model = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poses.last().pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        for (Layer layer : mesh.layers) {
            layer.type.setupRenderState();
            var shader = RenderSystem.getShader();
            if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
            layer.buffer.bind();
            layer.buffer.drawWithShader(model, projection, shader);
            VertexBuffer.unbind();
            layer.type.clearRenderState();
        }
        Minecraft minecraft = Minecraft.getInstance();
        for (Dynamic dynamic : mesh.dynamic) {
            poses.pushPose();
            poses.translate(dynamic.pos.getX(), dynamic.pos.getY(), dynamic.pos.getZ());
            minecraft.getBlockRenderer().renderSingleBlock(dynamic.state, poses, graphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY);
            poses.popPose();
        }
        graphics.flush();
        poses.popPose();
        Lighting.setupFor3DItems();
        graphics.disableScissor();
    }

    private void ensureMesh(List<DockView.PreviewBlock> blocks) {
        if (meshSource == blocks && mesh != null) return;
        close();
        meshSource = blocks;
        mesh = bake(blocks);
    }

    private static Mesh bake(List<DockView.PreviewBlock> blocks) {
        Minecraft minecraft = Minecraft.getInstance();
        Map<BlockPos, BlockState> states = new HashMap<>();
        Map<RenderType, List<DockView.PreviewBlock>> byType = new LinkedHashMap<>();
        List<Dynamic> dynamic = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxBlockY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int maxY = 1;
        for (DockView.PreviewBlock block : blocks) {
            states.put(block.pos(), block.state());
            minX = Math.min(minX, block.pos().getX()); maxX = Math.max(maxX, block.pos().getX());
            minY = Math.min(minY, block.pos().getY()); maxBlockY = Math.max(maxBlockY, block.pos().getY());
            minZ = Math.min(minZ, block.pos().getZ()); maxZ = Math.max(maxZ, block.pos().getZ());
            maxY = Math.max(maxY, block.pos().getY() + 1);
            RenderType layer = ItemBlockRenderTypes.getChunkRenderType(block.state());
            if (block.state().getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED || layer == RenderType.translucent())
                dynamic.add(new Dynamic(block.pos(), block.state()));
            else if (block.state().getRenderShape() == RenderShape.MODEL)
                byType.computeIfAbsent(layer, ignored -> new ArrayList<>()).add(block);
        }
        PreviewView view = new PreviewView(Map.copyOf(states), maxY);
        List<Layer> layers = new ArrayList<>();
        try {
            for (Map.Entry<RenderType, List<DockView.PreviewBlock>> entry : byType.entrySet()) {
                try (ByteBufferBuilder bytes = new ByteBufferBuilder(4 * 1024 * 1024)) {
                    BufferBuilder buffer = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                    PoseStack poses = new PoseStack();
                    for (DockView.PreviewBlock block : entry.getValue()) {
                        BlockPos pos = block.pos();
                        poses.pushPose();
                        poses.translate(pos.getX(), pos.getY(), pos.getZ());
                        minecraft.getBlockRenderer().getModelRenderer().tesselateBlock(view,
                                minecraft.getBlockRenderer().getBlockModel(block.state()), block.state(), pos,
                                poses, buffer, true, RandomSource.create(42), block.state().getSeed(pos), OverlayTexture.NO_OVERLAY);
                        poses.popPose();
                    }
                    MeshData data = buffer.build();
                    if (data != null) {
                        VertexBuffer vertex = new VertexBuffer(VertexBuffer.Usage.STATIC);
                        vertex.bind();
                        vertex.upload(data);
                        VertexBuffer.unbind();
                        layers.add(new Layer(entry.getKey(), vertex));
                    }
                }
            }
            return new Mesh(List.copyOf(layers), List.copyOf(dynamic), new Bounds(minX, minY, minZ, maxX, maxBlockY, maxZ));
        } catch (RuntimeException | Error failure) {
            layers.forEach(layer -> layer.buffer.close());
            throw failure;
        }
    }

    @Override
    public void close() {
        if (mesh != null) mesh.close();
        mesh = null;
        meshSource = null;
    }

    private record PreviewView(Map<BlockPos, BlockState> states, int height) implements BlockAndTintGetter {
        @Override public BlockState getBlockState(BlockPos pos) { return states.getOrDefault(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); }
        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
        @Override public int getHeight() { return height; }
        @Override public int getMinBuildHeight() { return 0; }
        @Override public float getShade(Direction direction, boolean shade) { return Minecraft.getInstance().level.getShade(direction, shade); }
        @Override public LevelLightEngine getLightEngine() { return Minecraft.getInstance().level.getLightEngine(); }
        @Override public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            Minecraft minecraft = Minecraft.getInstance();
            BlockPos origin = minecraft.player == null ? BlockPos.ZERO : minecraft.player.blockPosition();
            return minecraft.level.getBlockTint(origin.offset(pos), resolver);
        }
        @Override public int getBrightness(LightLayer layer, BlockPos pos) { return 15; }
    }
}
