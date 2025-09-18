package nl.mishaopstal.creeperextended.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;

public class FlashbangStatusEffect extends StatusEffect {
    /**
     * Custom flashbang effect used to drive a client-side overlay.
     *
     * Semantics:
     * - The StatusEffectInstance "amplifier" encodes the desired fade-in duration in ticks (0..127 recommended).
     * - Color and other visual params are read by the client from config.
     * - No periodic server updates are needed; client only checks presence + amplifier.
     */
    public FlashbangStatusEffect() {
        super(
            StatusEffectCategory.HARMFUL, // whether beneficial or harmful for entities
            0xFFFAFA // color in RGB (unused by our overlay, but shows in effect HUD)
        );
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // We don't need periodic server logic here, only presence matters.
        return false;
    }
}
