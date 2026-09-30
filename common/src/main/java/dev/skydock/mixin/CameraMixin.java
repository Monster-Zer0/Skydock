package dev.skydock.mixin;

import dev.skydock.client.ShipCamera;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow protected abstract void setPosition(Vec3 position);

    /** TAIL keeps NeoForge's camera-angle and detached-distance hooks intact; only the final position changes. */
    @Inject(method = "setup", at = @At("TAIL"))
    private void skydock$shipCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        Vec3 eye = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + Mth.lerp(partialTick, eyeHeightOld, eyeHeight), Mth.lerp(partialTick, entity.zo, entity.getZ()));
        Vec3 position = ShipCamera.position(level, entity, detached, eye, new Vec3(((Camera) (Object) this).getLookVector()), partialTick);
        if (position != null) setPosition(position);
    }
}
