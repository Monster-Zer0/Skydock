package dev.skydock.mixin;

import dev.skydock.client.SkydockClient;
import dev.skydock.network.ShipNetwork;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMovementMixin {
    /** Pair metadata only with concrete positional packets; rotation/status sends cannot orphan it. */
    @Redirect(method = "sendPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void skydock$sendDeckFrame(ClientPacketListener connection, Packet<?> packet) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!player.isPassenger() && packet instanceof ServerboundMovePlayerPacket movement && movement.hasPosition())
            ShipNetwork.deckMove(SkydockClient.movementFrame(player));
        connection.send(packet);
    }
}
