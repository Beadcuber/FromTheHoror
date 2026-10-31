package com.fromthehorror.client;

import com.fromthehorror.FromTheHorror;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class FromTheHorrorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(FromTheHorror.DWELLER, ctx -> new StalkerRenderer(ctx, "dweller", true));
        EntityRendererRegistry.register(FromTheHorror.WATCHER, ctx -> new StalkerRenderer(ctx, "watcher", false));
        Buddy.init();
    }
}
