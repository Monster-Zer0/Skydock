package dev.skydock.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class ShipBlockView implements BlockGetter {
    private final Ship ship;
    public ShipBlockView(Ship ship) { this.ship = ship; }
    @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Override public BlockState getBlockState(BlockPos pos) { return ship.state(pos); }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public int getHeight() { return ship.tier.height; }
    @Override public int getMinBuildHeight() { return 0; }
}
