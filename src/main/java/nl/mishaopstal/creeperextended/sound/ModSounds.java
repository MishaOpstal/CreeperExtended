package nl.mishaopstal.creeperextended.sound;

import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import nl.mishaopstal.creeperextended.CreeperExtended;

public class ModSounds {
    public static final SoundEvent BOMB_BEEP = registerSoundEvent("beep");
    public static final SoundEvent FLASHBANG_DETONATE = registerSoundEvent("flashbang_detonate");
    public static final SoundEvent FLASHBANG_THROW_01 = registerSoundEvent("am1_throwing_flashbang_01");
    public static final SoundEvent FLASHBANG_THROW_02 = registerSoundEvent("am1_throwing_flashbang_02");
    public static final SoundEvent FLASHBANG_THROW_02_1 = registerSoundEvent("am1_throwing_flashbang_02_1");
    public static final SoundEvent FLASHBANG_THROW_03 = registerSoundEvent("am1_throwing_flashbang_03");

    public static final SoundEvent SEMBER_MUSIC = registerSoundEvent("sember");
    public static final RegistryKey<JukeboxSong> SEMBER_MUSIC_KEY = RegistryKey.of(
            RegistryKeys.JUKEBOX_SONG,
            Identifier.of(CreeperExtended.MOD_ID, "sember")
    );

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(CreeperExtended.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void initialize() {
        CreeperExtended.LOGGER.info("Registering mod sounds");
    }
}
