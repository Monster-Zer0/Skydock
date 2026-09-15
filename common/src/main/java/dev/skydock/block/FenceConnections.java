package dev.skydock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.function.Function;

/** Resolves fence arms for authored or persisted block collections outside normal placement updates. */
public final class FenceConnections {
    private FenceConnections() {}

    public static BlockState resolve(BlockState state, BlockPos pos,
                                     Function<BlockPos, BlockState> states, BlockGetter shapeLevel) {
        if (!(state.getBlock() instanceof FenceBlock fence)) return state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = states.apply(neighborPos);
            Direction face = direction.getOpposite();
            boolean sturdy = neighbor.isFaceSturdy(shapeLevel, neighborPos, face);
            boolean connected = neighbor.getBlock() == fence || fence.connectsTo(neighbor, sturdy, face);
            state = state.setValue(property(direction), connected);
        }
        return state;
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> CrossCollisionBlock.NORTH;
            case EAST -> CrossCollisionBlock.EAST;
            case SOUTH -> CrossCollisionBlock.SOUTH;
            case WEST -> CrossCollisionBlock.WEST;
            default -> throw new IllegalArgumentException("Fence connection is not horizontal: " + direction);
        };
    }
}
