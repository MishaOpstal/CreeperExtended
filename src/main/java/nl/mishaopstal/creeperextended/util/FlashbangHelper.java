package nl.mishaopstal.creeperextended.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.sound.ModSounds;

public class FlashbangHelper {
    public static void playThrowFlashbangSound(World world, BlockPos pos) {
        // List of available sound identifiers
        SoundEvent[] soundOptions = {
            ModSounds.FLASHBANG_THROW_01,
            ModSounds.FLASHBANG_THROW_02,
            ModSounds.FLASHBANG_THROW_02_1,
            ModSounds.FLASHBANG_THROW_03
        };

        int randomIndex = (int) (Math.random() * soundOptions.length);
        SoundEvent selectedSound = soundOptions[randomIndex];
        ModHelpers.playSound(world, pos, selectedSound, SoundCategory.PLAYERS);
    }

    public static void playExplosionSound(World world, BlockPos pos) {
        ModHelpers.playSound(world, pos, ModSounds.FLASHBANG_DETONATE, SoundCategory.HOSTILE, CreeperExtended.CONFIG.flashbangVolume(), 1.0f);
    }

    public static void applyFlashEffect(LivingEntity entity, int baseDuration, int fadeInTicks) {
        RegistryEntry<StatusEffect> flashbangEffect = Registries.STATUS_EFFECT
                .getEntry(Identifier.of(CreeperExtended.MOD_ID, "flashbang_effect"))
                .orElseThrow(() -> new IllegalStateException("Flashbang effect not registered!"));

        ModHelpers.applyStatusEffect(entity, flashbangEffect, baseDuration, fadeInTicks);
    }

    public static void applySlownessEffect(LivingEntity entity, int duration, int amplifier) {
        // Use the built-in SLOWNESS reference from StatusEffects
        RegistryEntry<StatusEffect> slownessEffect = StatusEffects.SLOWNESS;
        ModHelpers.applyStatusEffect(entity, slownessEffect, duration, amplifier);
    }
}
