// ClientFlashOverlay.java (client-only utility)
package nl.mishaopstal.creeperextended.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import nl.mishaopstal.creeperextended.CreeperExtended;

public final class ClientFlashOverlay {

    // Static state on client to track current flash
    private static boolean flashing = false;
    private static long startTick = 0L;
    private static int totalDuration = 0; // in ticks
    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = FADE_IN_TICKS * 2;
    private static final int FULL_WHITE_TICKS = 40;  // tweak as desired

    private static RegistryEntry<net.minecraft.entity.effect.StatusEffect> FLASHBANG_ENTRY;

    private ClientFlashOverlay() {}

    public static void init() {
        // Resolve the registry entry for our status effect
        FLASHBANG_ENTRY = Registries.STATUS_EFFECT.getEntry(CreeperExtended.FLASHBANG_EFFECT);

        // Register tick callback
        ClientTickEvents.END_CLIENT_TICK.register(ClientFlashOverlay::onClientTick);

        // Register HUD render overlay
        HudRenderCallback.EVENT.register((DrawContext drawContext, RenderTickCounter tickCounter) -> {
            renderOverlay(drawContext);
        });
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || FLASHBANG_ENTRY == null) {
            // reset if no player/world
            flashing = false;
            return;
        }
        StatusEffectInstance inst = client.player.getStatusEffect(FLASHBANG_ENTRY);
        if (inst != null) {
            if (!flashing) {
                // Start flashing
                flashing = true;
                startTick = client.world.getTime();  // world time ticks
                totalDuration = inst.getDuration();  // remaining duration in ticks
            }
        } else {
            // Effect ended
            flashing = false;
        }
    }

    private static void renderOverlay(DrawContext drawContext) {
        if (!flashing) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        long currentTick = client.world.getTime();
        long elapsed = currentTick - startTick;
        if (elapsed < 0) elapsed = 0;

        float alpha;
        if (elapsed <= FADE_IN_TICKS) {
            alpha = (float) elapsed / (float) FADE_IN_TICKS;
        } else if (elapsed <= FADE_IN_TICKS + FULL_WHITE_TICKS) {
            alpha = 1.0f;
        } else {
            float outElapsed = elapsed - (FADE_IN_TICKS + FULL_WHITE_TICKS);
            alpha = 1.0f - (outElapsed / (float) FADE_OUT_TICKS);
        }

        alpha = MathHelper.clamp(alpha, 0f, 1f);

        // Draw full-screen white rectangle with alpha
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int intAlpha = (int) (alpha * 255.0f);
        int color = (intAlpha << 24) | 0xFFFFFF;

        // Using DrawContext; fill draws a colored quad over the HUD
        drawContext.fill(0, 0, width, height, color);
    }
}
