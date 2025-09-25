package nl.mishaopstal.creeperextended.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;

import java.util.Optional;

public class FlashbangHelper {
    public static void playThrowFlashbangSound(World world, BlockPos pos) {
        // List of available sound identifiers
        String[] soundOptions = {
            "creeperextended:am1_throwing_flashbang_01",
            "creeperextended:am1_throwing_flashbang_02",
            "creeperextended:am1_throwing_flashbang_02_1",
            "creeperextended:am1_throwing_flashbang_03"
        };

        int randomIndex = (int) (Math.random() * soundOptions.length);
        String selectedSound = soundOptions[randomIndex];
        ModHelpers.playSound(world, pos, selectedSound, SoundCategory.PLAYERS);
    }

    public static void playExplosionSound(World world, BlockPos pos) {
        ModHelpers.playSound(world, pos, "creeperextended:explosion_ringing", SoundCategory.HOSTILE, CreeperExtended.CONFIG.flashbangVolume(), 1.0f);
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
