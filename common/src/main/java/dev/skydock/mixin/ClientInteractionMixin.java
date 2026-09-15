package dev.skydock.mixin;

import dev.skydock.network.ShipNetwork;
import dev.skydock.ship.ShipInteractions;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Minecraft.class)
public abstract class ClientInteractionMixin {
    private boolean skydock$shipHit() { var mc = (Minecraft) (Object) this; return mc.player != null && ShipInteractions.pick(mc.player) != null; }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void skydock$use(CallbackInfo ci) { if (dev.skydock.client.SkydockClient.useViewedShip()) ci.cancel(); }
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void skydock$attack(CallbackInfoReturnable<Boolean> cir) { if (skydock$shipHit()) cir.setReturnValue(false); }
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void skydock$break(boolean down, CallbackInfo ci) { if (skydock$shipHit()) ci.cancel(); }
}
