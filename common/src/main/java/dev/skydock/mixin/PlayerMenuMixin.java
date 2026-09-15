package dev.skydock.mixin;

import dev.skydock.ship.ShipInteractions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Player.tick performs a second validity check during ServerPlayer.doTick. */
@Mixin(Player.class)
public abstract class PlayerMenuMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean skydock$remoteMenu(AbstractContainerMenu menu, Player player) {
        return ShipInteractions.menuStillValid(player, menu);
    }
}
