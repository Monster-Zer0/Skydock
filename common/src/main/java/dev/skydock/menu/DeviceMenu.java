package dev.skydock.menu;

import dev.architectury.registry.menu.MenuRegistry;
import dev.skydock.block.*;
import dev.skydock.data.MassTable;
import dev.skydock.ship.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Read-only instrument data plus the familiar primary action for active ship devices. */
public final class DeviceMenu extends AbstractContainerMenu {
    public record View(DeviceBlock.Kind kind, BlockPos pos, UUID ship, int amount, double value, int flags) {
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(kind.ordinal()); buffer.writeBlockPos(pos); buffer.writeBoolean(ship != null);
            if (ship != null) buffer.writeUUID(ship);
            buffer.writeVarInt(amount); buffer.writeDouble(value); buffer.writeVarInt(flags);
        }
        public static View read(FriendlyByteBuf buffer) {
            DeviceBlock.Kind kind = DeviceBlock.Kind.values()[Math.clamp(buffer.readVarInt(), 0, DeviceBlock.Kind.values().length - 1)];
            BlockPos pos = buffer.readBlockPos(); UUID ship = buffer.readBoolean() ? buffer.readUUID() : null;
            return new View(kind, pos, ship, buffer.readVarInt(), buffer.readDouble(), buffer.readVarInt());
        }
    }

    public final View view;
    private final ContainerData data;
    public DeviceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, View.read(buffer), new SimpleContainerData(5));
        seed(view.flags, view.amount, view.value, view.kind == DeviceBlock.Kind.HELM ? view.value : 0);
    }
    private DeviceMenu(int id, Inventory inventory, View view, ContainerData data) {
        super(SkydockBlocks.DEVICE_MENU.get(), id); this.view = view; this.data = data;
        checkContainerDataCount(data, 5); addDataSlots(data);
    }

    public static void openGround(ServerPlayer player, BlockPos pos, DeviceBlock.Kind kind, int amount, double value) {
        View view = new View(kind, pos.immutable(), null, amount, value, 0);
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider((id, inventory, ignored) ->
                new DeviceMenu(id, inventory, view, groundData(player, pos, kind)), title(kind)), view::write);
    }
    public static void openShip(ServerPlayer player, Ship ship, BlockPos local, DeviceBlock.Kind kind) {
        int flags = (ship.moored ? 1 : 0) | (ship.cruise ? 2 : 0) | (ship.pilot != null ? 4 : 0)
                | (player.getUUID().equals(ship.pilot) ? 8 : 0) | (ship.seated.containsValue(local) ? 16 : 0)
                | (local.equals(ship.seated.get(player.getUUID())) ? 32 : 0);
        int amount = kind == DeviceBlock.Kind.LIFT ? LiftCellBlock.clusterSize(new ShipBlockView(ship), local) : ship.blocks.size();
        double value = switch (kind) {
            case LIFT -> amount * MassTable.liftPerCell();
            case BALLAST -> MassTable.mass(ship.state(local));
            default -> ship.velocity.length() * 20;
        };
        View view = new View(kind, local.immutable(), ship.id, amount, value, flags);
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider((id, inventory, ignored) ->
                new DeviceMenu(id, inventory, view, shipData(player, ship, local, kind)), title(kind)), view::write);
    }
    private void seed(int flags, int amount, double value, double speed) {
        int measured = encode(value);
        data.set(0, flags); data.set(1, amount); data.set(2, measured & 0xffff); data.set(3, measured >>> 16); data.set(4, encode(speed));
    }
    private static int encode(double value) { return (int) Math.clamp(Math.round(value * 10), Integer.MIN_VALUE, Integer.MAX_VALUE); }
    public int flags() { return data.get(0); }
    public int amount() { return data.get(1); }
    public double value() { return (((data.get(3) & 0xffff) << 16) | (data.get(2) & 0xffff)) / 10.0; }
    public double speed() { return data.get(4) / 10.0; }
    private static ContainerData groundData(ServerPlayer player, BlockPos pos, DeviceBlock.Kind kind) {
        return dynamic(index -> {
            var state = player.level().getBlockState(pos);
            int amount = kind == DeviceBlock.Kind.LIFT && matches(state, kind) ? LiftCellBlock.clusterSize(player.level(), pos) : 1;
            return switch (index) {
                case 0 -> 0; case 1 -> amount;
                case 2 -> encode(kind == DeviceBlock.Kind.LIFT ? amount * MassTable.liftPerCell() : kind == DeviceBlock.Kind.BALLAST ? MassTable.mass(state) : 0) & 0xffff;
                case 3 -> encode(kind == DeviceBlock.Kind.LIFT ? amount * MassTable.liftPerCell() : kind == DeviceBlock.Kind.BALLAST ? MassTable.mass(state) : 0) >>> 16;
                default -> 0;
            };
        });
    }
    private static ContainerData shipData(ServerPlayer player, Ship ship, BlockPos local, DeviceBlock.Kind kind) {
        return dynamic(index -> {
            int flags = (ship.moored ? 1 : 0) | (ship.cruise ? 2 : 0) | (ship.pilot != null ? 4 : 0)
                    | (player.getUUID().equals(ship.pilot) ? 8 : 0) | (ship.seated.containsValue(local) ? 16 : 0)
                    | (local.equals(ship.seated.get(player.getUUID())) ? 32 : 0);
            int amount = kind == DeviceBlock.Kind.LIFT ? LiftCellBlock.clusterSize(new ShipBlockView(ship), local) : ship.blocks.size();
            double value = switch (kind) {
                case LIFT -> amount * MassTable.liftPerCell(); case BALLAST -> MassTable.mass(ship.state(local)); default -> 0;
            };
            int measured = encode(value);
            return switch (index) { case 0 -> flags; case 1 -> amount; case 2 -> measured & 0xffff; case 3 -> measured >>> 16; default -> encode(ship.velocity.length() * 20); };
        });
    }
    private static ContainerData dynamic(java.util.function.IntUnaryOperator getter) {
        return new ContainerData() {
            @Override public int get(int index) { return getter.applyAsInt(index); }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 5; }
        };
    }
    private static Component title(DeviceBlock.Kind kind) {
        return Component.translatable(switch (kind) {
            case HELM -> "block.skydock.helm"; case CLAMP -> "block.skydock.mooring_clamp"; case SEAT -> "block.skydock.seat";
            case LIFT -> "block.skydock.lift_cell"; case BALLAST -> "block.skydock.ballast";
        });
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        if (view.ship != null) return true; // Moving-ship reach and permissions are enforced by ShipInteractions' menu guard.
        return player.isAlive() && matches(player.level().getBlockState(view.pos), view.kind)
                && player.distanceToSqr(view.pos.getX() + .5, view.pos.getY() + .5, view.pos.getZ() + .5) <= 64;
    }
    private static boolean matches(net.minecraft.world.level.block.state.BlockState state, DeviceBlock.Kind kind) {
        return switch (kind) {
            case HELM -> state.is(SkydockBlocks.HELM.get()); case CLAMP -> state.is(SkydockBlocks.CLAMP.get());
            case SEAT -> state.is(SkydockBlocks.SEAT.get()); case LIFT -> state.is(SkydockBlocks.LIFT_CELLS);
            case BALLAST -> state.is(SkydockBlocks.BALLAST.get());
        };
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || view.ship == null || !(player instanceof ServerPlayer serverPlayer)) return false;
        boolean used = ShipInteractions.activateFromMenu(serverPlayer, view.ship, view.pos, view.kind);
        if (used) player.closeContainer();
        return used;
    }
}
