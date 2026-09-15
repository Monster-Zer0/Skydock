package dev.skydock.neoforge;

import dev.skydock.Skydock;
import net.neoforged.fml.common.Mod;

@Mod(Skydock.ID)
public final class SkydockNeoForge {
    public SkydockNeoForge() {
        Skydock.init();
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) SkydockNeoForgeClient.init();
    }
}
