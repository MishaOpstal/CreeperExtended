// ClientFlashOverlay.java (client-only utility)
package nl.mishaopstal.creeperextended.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;

public final class StunnedStatusTick {

    // Static state on client to track current stun
    private static boolean stunned = false;
    private static float currentIntensity = 0f;

    private StunnedStatusTick() {}

    public static void initialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() == Items.MILK_BUCKET) {
                if (player == MinecraftClient.getInstance().player) {
                    stunned = false;
                    currentIntensity = 0f;
                }
            }
            return ActionResult.PASS;
        });

        registerHudOverlay();
    }

    static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || ClientTick.STUNNED_ENTRY == null) {
            // reset if no player/world
            stunned = false;
            return;
        }

        StatusEffectInstance inst = client.player.getStatusEffect(ClientTick.STUNNED_ENTRY);
        if (inst == null) {
            // Effect gone: clear signature to allow the next real trigger
            return;
        }

        int effDur = Math.max(0, inst.getDuration());

        // If we are stunned, apply the effect
        if (effDur > 0) {
            stunned = true;

            // Grayscale the screen
            float intensity = MathHelper.clamp(effDur / 100.0f, 0.0f, 1.0f);
            applyGrayscaleEffect(intensity);
        } else {
            stunned = false;
        }

        if (!stunned) {
            // Remove the effect when not stunned
            removeGrayscaleEffect();
        }
    }

    public static void registerHudOverlay() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!stunned || currentIntensity <= 0) return;

            MinecraftClient client = MinecraftClient.getInstance();
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();

            int alpha = (int)(currentIntensity * 180);
            int color = (alpha << 24) | 0x808080;
            drawContext.fill(0, 0, width, height, color);
        });
    }

    private static void applyGrayscaleEffect(float intensity) {
        currentIntensity = intensity;
    }

    private static void removeGrayscaleEffect() {
        currentIntensity = 0f;
    }
}
