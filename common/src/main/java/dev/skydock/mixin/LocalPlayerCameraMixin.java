package dev.skydock.mixin;

import dev.skydock.client.CarriedCameraMotion;
import dev.skydock.client.SkydockClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerCameraMixin {
    @Inject(method = "getViewYRot", at = @At("RETURN"), cancellable = true)
    private void skydock$smoothCarriedYaw(float partialTick, CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        float carriedTurn = SkydockClient.carriedYaw();
        if (!player.isPassenger() && carriedTurn != 0)
            cir.setReturnValue(CarriedCameraMotion.interpolateYaw(cir.getReturnValue(), carriedTurn, partialTick));
    }
}
