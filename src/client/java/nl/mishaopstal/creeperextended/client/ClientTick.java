// ClientFlashOverlay.java (client-only utility)
package nl.mishaopstal.creeperextended.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import nl.mishaopstal.creeperextended.CreeperExtended;

public final class ClientTick {
    static RegistryEntry<net.minecraft.entity.effect.StatusEffect> FLASHBANG_ENTRY;
    static RegistryEntry<net.minecraft.entity.effect.StatusEffect> STUNNED_ENTRY;
    private ClientTick() {}

    public static void initialize() {
        // Resolve the registry entry for our status effect
        FLASHBANG_ENTRY = Registries.STATUS_EFFECT.getEntry(CreeperExtended.FLASHBANG_EFFECT);
        STUNNED_ENTRY = Registries.STATUS_EFFECT.getEntry(CreeperExtended.STUNNED_EFFECT);

        // initialize tick callbacks
        ClientFlashOverlay.initialize();
        StunnedStatusTick.initialize();

        // Register client tick events
        ClientTickEvents.END_CLIENT_TICK.register(ClientFlashOverlay::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(StunnedStatusTick::onClientTick);
    }
}
