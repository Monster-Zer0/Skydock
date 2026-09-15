package dev.skydock.mixin;

import dev.skydock.ship.ShipAttachment;
import dev.skydock.ship.ShipAttachmentAccess;
import dev.skydock.ship.ShipCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin implements ShipAttachmentAccess {
    @Unique private final ShipAttachment skydock$attachment = new ShipAttachment();

    @Override public ShipAttachment skydock$attachment() { return skydock$attachment; }

    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void skydock$collide(Vec3 motion, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(ShipCollision.collide((Entity) (Object) this, cir.getReturnValue()));
    }
}
