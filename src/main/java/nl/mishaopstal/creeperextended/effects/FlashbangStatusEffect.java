package nl.mishaopstal.creeperextended.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;

public class FlashbangStatusEffect extends StatusEffect {
    public FlashbangStatusEffect() {
        super(
            StatusEffectCategory.HARMFUL, // whether beneficial or harmful for entities
            0xFFFAFA // color in RGB
        );
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // We don't need periodic server logic here, only presence matters.
        return false;
    }
}
