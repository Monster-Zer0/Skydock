package dev.skydock.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

public final class DirectionalDecorationBlock extends HorizontalDirectionalBlock {
    public enum Kind { AWNING, FLAG }
    private final Kind kind;
    public DirectionalDecorationBlock(Kind kind, Properties properties) {
        super(properties); this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<DirectionalDecorationBlock> codec() { return MapCodec.unit(this); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape north = switch (kind) {
            case AWNING -> Shapes.or(box(0, 13, 0, 16, 16, 16), box(1, 0, 13, 3, 13, 15), box(13, 0, 13, 15, 13, 15));
            case FLAG -> Shapes.or(box(7, 0, 7, 9, 16, 9), box(8, 7, 8, 16, 15, 9));
        };
        return switch (state.getValue(FACING)) {
            case EAST -> rotate(north, 1); case SOUTH -> rotate(north, 2); case WEST -> rotate(north, 3); default -> north;
        };
    }
    private static VoxelShape rotate(VoxelShape shape, int turns) {
        VoxelShape result = shape;
        for (int i = 0; i < turns; i++) {
            VoxelShape source = result; result = Shapes.empty();
            for (var box : source.toAabbs()) result = Shapes.or(result, Shapes.create(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX));
        }
        return result;
    }
}
