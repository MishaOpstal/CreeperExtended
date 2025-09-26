package nl.mishaopstal.creeperextended.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.sound.ModSounds;

public class ModItems {
    public static final RegistryKey<Item> FLASHBANG_ITEM_KEY =
            RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(CreeperExtended.MOD_ID, "flashbang"));

    public static final RegistryKey<Item> SEMBER_MUSIC_DISC_ITEM_KEY =
            RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(CreeperExtended.MOD_ID, "sember_music_disc"));

    public static final Item FLASHBANG = registerItem(
            "flashbang",
            new FlashbangItem(new Item.Settings().registryKey(FLASHBANG_ITEM_KEY).maxCount(16))
    );

    public static final Item SEMBER_MUSIC_DISC = registerItem(
            "sember_music_disc",
            new Item(new Item.Settings().jukeboxPlayable(ModSounds.SEMBER_MUSIC_KEY).registryKey(SEMBER_MUSIC_DISC_ITEM_KEY).maxCount(1))
    );

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(CreeperExtended.MOD_ID, name), item);
    }

    public static void initialize() {
        CreeperExtended.LOGGER.info("Registering mod items");
    }
}