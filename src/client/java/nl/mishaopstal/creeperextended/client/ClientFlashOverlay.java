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
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import nl.mishaopstal.creeperextended.CreeperExtended;

public final class ClientFlashOverlay {

    // Static state on client to track current flash
    private static boolean flashing = false;
    private static long startTick = 0L;
    private static int totalDuration = 0; // in ticks from when effect detected
    private static int fadeInTicks = 10; // configurable via amplifier
    private static int colorRGB = 0xFFFFFF; // configurable via config

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
                totalDuration = inst.getDuration();  // total effect duration from start
                client.player.playSound(SoundEvent.of(Identifier.of("creeperextended:explosion_ringing")), CreeperExtended.CONFIG.flashBangVolume(), 1.0f);
            }
            // Update dynamic parameters from effect/config
            int amp = inst.getAmplifier();
            fadeInTicks = amp > 0 ? amp : CreeperExtended.getFlashBangFadeInTicks();
            colorRGB = CreeperExtended.getFlashBangColor();
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

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int tFadeIn = Math.max(1, fadeInTicks);
        int tTotal = Math.max(tFadeIn + 1, totalDuration > 0 ? totalDuration : tFadeIn + 60);

        float alpha;
        if (elapsed <= tFadeIn) {
            alpha = (float) elapsed / (float) tFadeIn;
        } else {
            float outProg = MathHelper.clamp((float) (elapsed - tFadeIn) / (float) (tTotal - tFadeIn), 0f, 1f);
            // Slow fade-out using sqrt curve
            alpha = 1.0f - (float) Math.sqrt(outProg);
        }
        alpha = MathHelper.clamp(alpha, 0f, 1f);

        // Compose main overlay color
        int intAlpha = (int) (alpha * 255.0f);
        int baseColor = (colorRGB & 0xFFFFFF);
        int mainColor = (intAlpha << 24) | baseColor;

        // Main overlay covers full screen
        drawContext.fill(0, 0, width, height, mainColor);

        // Ghosted double/triple layers that converge over time
        float outProg = MathHelper.clamp((float) Math.max(0, (int) elapsed - tFadeIn) / (float) (tTotal - tFadeIn), 0f, 1f);
        float ghostPhase = 1.0f - outProg; // 1 at start of fade-out -> 0 at end
        if (ghostPhase > 0f) {
            // Up to 3 ghost layers while fading out; fewer as it stabilizes
            int ghosts = ghostPhase > 0.66f ? 3 : (ghostPhase > 0.33f ? 2 : 1);
            float offsetMag = 6.0f * ghostPhase; // pixels
            int xOff = Math.max(1, (int) Math.round(offsetMag));
            int yOsc = (int) Math.round(Math.sin((currentTick % 360) * 0.25f) * (offsetMag * 0.5f));

            // Helper to tint color slightly per ghost
            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8) & 0xFF;
            int b = baseColor & 0xFF;

            float ghostAlphaBase = MathHelper.clamp(alpha * 0.6f * ghostPhase, 0f, 1f);

            // Left ghost (slightly more red)
            if (ghosts >= 1) {
                int rL = Math.min(255, (int) (r * 1.05f) + 10);
                int gL = Math.max(0, (int) (g * 0.95f));
                int bL = Math.max(0, (int) (b * 0.95f));
                int aL = (int) (ghostAlphaBase * 255);
                int colorL = (aL << 24) | (rL << 16) | (gL << 8) | bL;
                drawContext.fill(-xOff, yOsc, width - xOff, height + yOsc, colorL);
            }
            // Right ghost (slightly more blue)
            if (ghosts >= 2) {
                int rR = Math.max(0, (int) (r * 0.95f));
                int gR = Math.max(0, (int) (g * 0.95f));
                int bR = Math.min(255, (int) (b * 1.05f) + 10);
                int aR = (int) (ghostAlphaBase * 0.9f * 255);
                int colorR = (aR << 24) | (rR << 16) | (gR << 8) | bR;
                drawContext.fill(xOff, -yOsc, width + xOff, height - yOsc, colorR);
            }
            // Center ghost (slightly more green)
            if (ghosts >= 3) {
                int rC = Math.max(0, (int) (r * 0.95f));
                int gC = Math.min(255, (int) (g * 1.05f) + 10);
                int bC = Math.max(0, (int) (b * 0.95f));
                int aC = (int) (ghostAlphaBase * 0.7f * 255);
                int colorC = (aC << 24) | (rC << 16) | (gC << 8) | bC;
                int yOff = -yOsc / 2;
                drawContext.fill(0, yOff, width, height + yOff, colorC);
            }
        }
    }
}
