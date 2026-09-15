package dev.skydock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import dev.skydock.ship.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Locale;
import static net.minecraft.commands.Commands.*;

public final class SkydockCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = literal("skydock").executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.literal("Skydock: inspect / launch / redock <controller x y z>; use; release; cruise; control <thrust -1..1> <yaw -1..1> <climb -1..1>; status"), false); return 1;
        });
        root.then(literal("inspect").then(argument("controller", BlockPosArgument.blockPos()).executes(ctx -> { ShipManager.inspect(ctx.getSource().getPlayerOrException(), BlockPosArgument.getLoadedBlockPos(ctx, "controller")); return 1; })));
        root.then(literal("launch").then(argument("controller", BlockPosArgument.blockPos()).executes(ctx -> { ShipManager.launch(ctx.getSource().getPlayerOrException(), BlockPosArgument.getLoadedBlockPos(ctx, "controller")); return 1; })));
        root.then(literal("redock").then(argument("controller", BlockPosArgument.blockPos()).executes(ctx -> { ShipManager.redockNearest(ctx.getSource().getPlayerOrException(), BlockPosArgument.getLoadedBlockPos(ctx, "controller")); return 1; })));
        root.then(literal("use").executes(ctx -> { ShipInteractions.useFromLook(ctx.getSource().getPlayerOrException()); return 1; }));
        root.then(literal("target").executes(ctx -> {
            var hit = ShipInteractions.pick(ctx.getSource().getPlayerOrException());
            ctx.getSource().sendSuccess(() -> Component.literal(hit == null ? "No ship block in reach." :
                    hit.ship().id + " | local=" + hit.localHit().getBlockPos().toShortString() + " | " + hit.ship().state(hit.localHit().getBlockPos()) +
                            " | reach=" + Math.sqrt(hit.distance())), false);
            return hit == null ? 0 : 1;
        }));
        root.then(literal("release").executes(ctx -> { ShipManager.release(ctx.getSource().getPlayerOrException()); return 1; }));
        root.then(literal("cruise").executes(ctx -> { ShipManager.toggleCruise(ctx.getSource().getPlayerOrException()); return 1; }));
        root.then(literal("control").then(argument("thrust", FloatArgumentType.floatArg(-1, 1)).then(argument("yaw", FloatArgumentType.floatArg(-1, 1)).then(argument("climb", FloatArgumentType.floatArg(-1, 1)).executes(ctx -> {
            ShipManager.control(ctx.getSource().getPlayerOrException(), FloatArgumentType.getFloat(ctx, "thrust"), FloatArgumentType.getFloat(ctx, "yaw"), FloatArgumentType.getFloat(ctx, "climb")); return 1;
        })))));
        root.then(literal("status").executes(ctx -> {
            var ships = ShipManager.ships(ctx.getSource().getLevel());
            ctx.getSource().sendSuccess(() -> Component.literal("Skydock ships: " + ships.size()), false);
            for (Ship ship : ships) ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "%s | %s | blocks=%d mass=%.0f lift=%.0f | pose=%.3f,%.3f,%.3f yaw=%.3f | velocity=%.3f,%.3f,%.3f | yard=%s | cruise=%s target=%.3f turn=%.3f | moored=%s blocked=%s pilot=%s",
                    ship.id, ship.tier, ship.blocks.size(), ship.mass, ship.lift, ship.pose.x(), ship.pose.y(), ship.pose.z(), ship.pose.yaw(),
                    ship.velocity.x, ship.velocity.y, ship.velocity.z, ship.yard.toShortString(), ship.cruise, ship.cruiseSpeed, ship.yawVelocity, ship.moored, ship.blocked, ship.pilot)), false);
            return ships.size();
        }));
        dispatcher.register(root);
    }
}
