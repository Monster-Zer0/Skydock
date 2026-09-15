package dev.skydock.mixin;

import dev.skydock.ship.ShipInteractions;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMenuPacketMixin {
    @Redirect(method = {"handleContainerClick", "handlePlaceRecipe", "handleContainerButtonClick", "handleSetBeaconPacket"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean skydock$remoteMenu(AbstractContainerMenu menu, Player player) { return ShipInteractions.menuStillValid(player, menu); }
}
