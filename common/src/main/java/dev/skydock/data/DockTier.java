package dev.skydock.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public enum DockTier {
    SCOUT(32, 20, 32, 2048), BRIG(48, 28, 48, 6144),
    CRUISER(64, 36, 64, 12288), DREADNOUGHT(96, 48, 96, 24576);

    public final int width, height, length, defaultCap;
    DockTier(int width, int height, int length, int cap) {
        this.width = width; this.height = height; this.length = length; this.defaultCap = cap;
    }
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    // Controller is centered on the front edge, one block below the interior floor.
    public BlockPos origin(BlockPos controller) { return controller.offset(-width / 2, 1, 1); }
    public AABB envelope(BlockPos controller) {
        BlockPos min = origin(controller);
        return new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + width, min.getY() + height, min.getZ() + length);
    }
}
