package dev.skydock.client;

import dev.skydock.ship.Ship;
import dev.skydock.ship.ShipPose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import java.util.Map;

/**
 * A hull as seen by the mesh builder, safe to read off the render thread: the ship's own blocks, full sky
 * light, each block's own glow, and the biome under the ship when the view was taken. A published hull's
 * block map is never written again, so it is shared rather than copied.
 */
public final class ShipRenderView implements BlockAndTintGetter {
    private static final LevelLightEngine NO_LIGHT = new LevelLightEngine(new LightChunkGetter() {
        @Override public LightChunk getChunkForLighting(int x, int z) { return null; }
        @Override public BlockGetter getLevel() { return EmptyBlockGetter.INSTANCE; }
    }, false, false);
    private final Map<BlockPos, BlockState> blocks;
    private final int height;
    private final float[] shades = new float[12];
    private final Biome biome;
    private final ShipPose.Transform transform;
    private final Vec3 center;

    /** Call on the render thread; the result can then be read from any thread. */
    public ShipRenderView(Ship ship, ClientLevel level) {
        blocks = ship.blocks; height = ship.tier.height; transform = ship.pose.transform(); center = ship.center();
        for (Direction direction : Direction.values()) {
            shades[direction.ordinal() * 2] = level.getShade(direction, false);
            shades[direction.ordinal() * 2 + 1] = level.getShade(direction, true);
        }
        biome = level.getBiome(BlockPos.containing(ship.pose.x(), ship.pose.y(), ship.pose.z())).value();
    }
    public Map<BlockPos, BlockState> blocks() { return blocks; }
    @Override public BlockState getBlockState(BlockPos p) { return blocks.getOrDefault(p, Blocks.AIR.defaultBlockState()); }
    @Override public FluidState getFluidState(BlockPos p) { return getBlockState(p).getFluidState(); }
    @Override public BlockEntity getBlockEntity(BlockPos p) { return null; }
    @Override public int getHeight() { return height; }
    @Override public int getMinBuildHeight() { return 0; }
    @Override public float getShade(Direction direction, boolean shade) { return shades[direction.ordinal() * 2 + (shade ? 1 : 0)]; }
    @Override public LevelLightEngine getLightEngine() { return NO_LIGHT; }
    @Override public int getBlockTint(BlockPos p, ColorResolver resolver) {
        Vec3 world = transform.toWorld(new Vec3(p.getX() + .5 - center.x, 0, p.getZ() + .5 - center.z));
        return resolver.getColor(biome, world.x, world.z);
    }
    @Override public int getBrightness(LightLayer layer, BlockPos p) { return layer == LightLayer.SKY ? 15 : getBlockState(p).getLightEmission(); }
}
