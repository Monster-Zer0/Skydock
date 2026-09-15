package dev.skydock.mixin;

import dev.skydock.ship.ShipCollision;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEdgeMixin {
    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void skydock$sneak(Vec3 motion, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        Player player = (Player) (Object) this;
        if (player.isShiftKeyDown() && player.onGround() && !player.getAbilities().flying) {
            Vec3 result = ShipCollision.sneak(player, motion); if (result != null) cir.setReturnValue(result);
        }
    }
}
