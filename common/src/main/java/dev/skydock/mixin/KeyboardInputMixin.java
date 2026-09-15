package dev.skydock.mixin;

import dev.skydock.client.SkydockClient;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void skydock$pilot(boolean slow, float factor, CallbackInfo ci) {
        if (SkydockClient.piloted() != null || SkydockClient.seated()) { leftImpulse = 0; forwardImpulse = 0; jumping = false; shiftKeyDown = false; }
    }
}
