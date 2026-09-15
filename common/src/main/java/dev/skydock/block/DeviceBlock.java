package dev.skydock.block;

import com.mojang.serialization.MapCodec;
import dev.skydock.ship.ShipManager;
import dev.skydock.data.MassTable;
import dev.skydock.menu.DeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class DeviceBlock extends HorizontalDirectionalBlock {
    public enum Kind { HELM, CLAMP, SEAT, LIFT, BALLAST }
    public final Kind kind;
    public DeviceBlock(Kind kind, Properties props) { super(props); this.kind = kind; registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH)); }
    @Override protected MapCodec<DeviceBlock> codec() { return MapCodec.unit(this); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (kind) {
            case SEAT -> box(2, 0, 2, 14, 8, 14);
            // The helm's wheel rises above one block; keep its collision enclosure
            // in sync with the authored 20-unit-tall profile.
            case HELM -> box(1, 0, 1, 15, 20, 15);
            default -> super.getShape(state, level, pos, context);
        };
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown()) DeviceMenu.openGround(sp, pos, kind, 1, kind == Kind.BALLAST ? MassTable.mass(state) : 0);
            else if (kind == Kind.HELM || kind == Kind.CLAMP || kind == Kind.SEAT) ShipManager.deviceOnGround(sp, kind, pos);
        }
        if (!player.isShiftKeyDown() && (kind == Kind.LIFT || kind == Kind.BALLAST)) return InteractionResult.PASS;
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
