package nl.mishaopstal.creeperextended.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class ModEntities {
    public static void initialize() {
        // Register entity renderers
        EntityRendererRegistry.register(ModEntityTypes.THROWN_FLASHBANG, FlyingItemEntityRenderer::new);
    }
}
