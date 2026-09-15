package dev.skydock.menu;

import dev.skydock.block.*;
import dev.skydock.network.DockNetwork;
import dev.skydock.ship.ShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class DockMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public DockMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, data.readBlockPos()); }
    public DockMenu(int id, Inventory inventory, BlockPos pos) { super(SkydockBlocks.DOCK_MENU.get(), id); this.pos = pos; }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof DockBlockEntity dock && dock.canManage(player) && ShipManager.canReachDock(player, dock);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        return player instanceof ServerPlayer sp && DockNetwork.handleMenuButton(sp, this, button);
    }
}
