// ClientFlashOverlay.java (client-only utility)
package nl.mishaopstal.creeperextended.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import nl.mishaopstal.creeperextended.CreeperExtended;

public final class ClientFlashOverlay {

    // Static state on client to track current flash
    private static boolean flashing = false;
    private static long startTick = 0L;
    private static int hold = 0; // in ticks from when effect detected
    private static int fadeInTicks = 10; // configurable via amplifier
    private static int fadeOutTicks = 10;
    private static int colorRGB = 0xFFFFFF; // configurable via config

    private static RegistryEntry<net.minecraft.entity.effect.StatusEffect> FLASHBANG_ENTRY;

    // Signature of the last seen effect instance to avoid retrigger during the same effect
    private static long lastEffectEndTickSig = -1L;
    private static int lastEffectAmplifierSig = -1;

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

        // Register effect removal listener
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() == Items.MILK_BUCKET) {
                if (player == MinecraftClient.getInstance().player) {
                    // stop immediately
                    flashing = false;
                    lastEffectEndTickSig = -1L;
                    lastEffectAmplifierSig = -1;
                }
            }
            return ActionResult.PASS;
        });
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || FLASHBANG_ENTRY == null) {
            // reset if no player/world
            flashing = false;
            lastEffectEndTickSig = -1L;
            lastEffectAmplifierSig = -1;
            return;
        }

        StatusEffectInstance inst = client.player.getStatusEffect(FLASHBANG_ENTRY);
        long worldTime = client.world.getTime();

        if (inst == null) {
            // Effect gone: clear signature to allow next real trigger
            lastEffectEndTickSig = -1L;
            lastEffectAmplifierSig = -1;
            return;
        }

        int amp = inst.getAmplifier();
        long effectEndTickNow = worldTime + inst.getDuration();

        boolean isNewEffect = (amp != lastEffectAmplifierSig) || (effectEndTickNow > lastEffectEndTickSig + 1L);
        if (isNewEffect) {
            // Determine base phase lengths from config and amplifier
            int baseFadeIn = amp > 0 ? amp : CreeperExtended.getFlashBangFadeInTicks();
            int baseHold = CreeperExtended.getFlashBangHold();
            int baseFadeOut = CreeperExtended.getFlashBangFadeOutTicks();
            baseFadeIn = Math.max(0, baseFadeIn);
            baseHold = Math.max(0, baseHold);
            baseFadeOut = Math.max(0, baseFadeOut);

            int baseTotal = baseFadeIn + baseHold + baseFadeOut;
            int effDur = Math.max(0, inst.getDuration());

            int tFi, tHo, tFo;
            if (baseTotal <= 0) {
                tFi = tHo = tFo = 0;
            } else {
                int targetTotal = Math.min(effDur, baseTotal);
                if (targetTotal == baseTotal) {
                    tFi = baseFadeIn;
                    tHo = baseHold;
                    tFo = baseFadeOut;
                } else {
                    float ratio = targetTotal / (float) baseTotal;
                    tFi = Math.round(baseFadeIn * ratio);
                    tHo = Math.round(baseHold * ratio);
                    tFo = targetTotal - tFi - tHo; // ensure sum matches exactly
                    if (tFo < 0) { tHo = Math.max(0, tHo + tFo); tFo = 0; }
                    if (tHo < 0) { tFi = Math.max(0, tFi + tHo); tHo = 0; }
                    if (tFi < 0) { tFi = 0; }
                }
            }

            // Start/restart overlay
            startTick = worldTime;
            fadeInTicks = tFi;
            hold = tHo;
            fadeOutTicks = tFo;
            colorRGB = CreeperExtended.getFlashBangColor();

            flashing = (fadeInTicks + hold + fadeOutTicks) > 0;
            client.player.playSound(SoundEvent.of(Identifier.of("creeperextended:explosion_ringing")), CreeperExtended.CONFIG.flashBangVolume(), 1.0f);

            // Update signature to block retrigger during the same effect instance
            lastEffectAmplifierSig = amp;
            lastEffectEndTickSig = effectEndTickNow;
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

        // Use cached timings computed at trigger time; allow zeros
        int tFadeIn = Math.max(0, fadeInTicks);
        int tHold = Math.max(0, hold);
        int tFadeOut = Math.max(0, fadeOutTicks);
        int total = tFadeIn + tHold + tFadeOut;
        if (total <= 0) {
            flashing = false;
            return;
        }

        float alpha;
        if (elapsed < tFadeIn) {
            // Fade in (0 → 1)
            alpha = (tFadeIn == 0) ? 1.0f : (float) elapsed / (float) tFadeIn;
        } else if (elapsed < (tFadeIn + tHold)) {
            // Hold at full brightness
            alpha = 1.0f;
        } else if (elapsed < total) {
            // Fade out (1 → 0)
            if (tFadeOut == 0) {
                alpha = 0.0f;
            } else {
                float outProg = (float) (elapsed - (tFadeIn + tHold)) / (float) tFadeOut;
                alpha = 1.0f - (float) Math.sqrt(MathHelper.clamp(outProg, 0f, 1f));
            }
        } else {
            // Finished
            flashing = false;
            return;
        }

        alpha = MathHelper.clamp(alpha, 0f, 1f);

        // Compose main overlay color
        int intAlpha = (int) (alpha * 255.0f);
        int baseColor = (colorRGB & 0xFFFFFF);
        int mainColor = (intAlpha << 24) | baseColor;

        // Main overlay
        drawContext.fill(0, 0, width, height, mainColor);

        // --- Ghosting effect (only during fade-out) ---
        if (tFadeOut > 0 && elapsed >= tFadeIn + tHold && elapsed < total) {
            float outProg = MathHelper.clamp((float) (elapsed - (tFadeIn + tHold)) / (float) tFadeOut, 0f, 1f);
            float ghostPhase = 1.0f - outProg; // 1 at start of fade-out → 0 at end
            if (ghostPhase > 0f) {
                int ghosts = ghostPhase > 0.66f ? 3 : (ghostPhase > 0.33f ? 2 : 1);
                float offsetMag = 6.0f * ghostPhase;
                int xOff = Math.max(1, (int) Math.round(offsetMag));
                int yOsc = (int) Math.round(Math.sin((currentTick % 360) * 0.25f) * (offsetMag * 0.5f));

                int r = (baseColor >> 16) & 0xFF;
                int g = (baseColor >> 8) & 0xFF;
                int b = baseColor & 0xFF;
                float ghostAlphaBase = MathHelper.clamp(alpha * 0.6f * ghostPhase, 0f, 1f);

                // Left ghost (more red)
                if (ghosts >= 1) {
                    int rL = Math.min(255, (int) (r * 1.05f) + 10);
                    int gL = Math.max(0, (int) (g * 0.95f));
                    int bL = Math.max(0, (int) (b * 0.95f));
                    int aL = (int) (ghostAlphaBase * 255);
                    int colorL = (aL << 24) | (rL << 16) | (gL << 8) | bL;
                    drawContext.fill(-xOff, yOsc, width - xOff, height + yOsc, colorL);
                }
                // Right ghost (more blue)
                if (ghosts >= 2) {
                    int rR = Math.max(0, (int) (r * 0.95f));
                    int gR = Math.max(0, (int) (g * 0.95f));
                    int bR = Math.min(255, (int) (b * 1.05f) + 10);
                    int aR = (int) (ghostAlphaBase * 0.9f * 255);
                    int colorR = (aR << 24) | (rR << 16) | (gR << 8) | bR;
                    drawContext.fill(xOff, -yOsc, width + xOff, height - yOsc, colorR);
                }
                // Center ghost (more green)
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
}
