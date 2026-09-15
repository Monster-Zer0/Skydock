package dev.skydock.neoforge;

import dev.skydock.client.*;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class SkydockNeoForgeClient {
    public static void init() {
        SkydockClient.init();
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
                ShipRenderer.render(event.getModelViewMatrix(), event.getProjectionMatrix(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        });
    }
}
