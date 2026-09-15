package dev.skydock.client;

import dev.skydock.ship.Ship;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class ShipRenderView implements BlockAndTintGetter {
    private final Ship ship;
    public ShipRenderView(Ship ship) { this.ship = ship; }
    @Override public BlockState getBlockState(BlockPos p) { return ship.state(p); }
    @Override public FluidState getFluidState(BlockPos p) { return ship.state(p).getFluidState(); }
    @Override public BlockEntity getBlockEntity(BlockPos p) { return null; }
    @Override public int getHeight() { return ship.tier.height; }
    @Override public int getMinBuildHeight() { return 0; }
    @Override public float getShade(Direction direction, boolean shade) { return Minecraft.getInstance().level.getShade(direction, shade); }
    @Override public LevelLightEngine getLightEngine() { return Minecraft.getInstance().level.getLightEngine(); }
    @Override public int getBlockTint(BlockPos p, ColorResolver resolver) { return Minecraft.getInstance().level.getBlockTint(worldPos(p), resolver); }
    @Override public int getBrightness(LightLayer layer, BlockPos p) {
        return layer == LightLayer.SKY ? 15 : Math.max(ship.state(p).getLightEmission(), Minecraft.getInstance().level.getBrightness(layer, worldPos(p)));
    }
    private BlockPos worldPos(BlockPos p) { return BlockPos.containing(ship.blockToWorld(Vec3.atCenterOf(p))); }
}
