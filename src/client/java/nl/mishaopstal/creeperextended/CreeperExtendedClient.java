package nl.mishaopstal.creeperextended;

import net.fabricmc.api.ClientModInitializer;
import nl.mishaopstal.creeperextended.client.ClientFlashOverlay;
import nl.mishaopstal.creeperextended.entity.ModEntities;
import nl.mishaopstal.creeperextended.entity.ModEntityTypes;
import nl.mishaopstal.creeperextended.item.ModItemGroups;
import nl.mishaopstal.creeperextended.item.ModItems;
import nl.mishaopstal.creeperextended.sound.ModSounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreeperExtendedClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CreeperExtended.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Creeper Extended (Client)");

        // Register game content first, then client overlays and callbacks
        ModItemGroups.initialize();
        ModItems.initialize();

        ModEntityTypes.initialize();
        ModEntities.initialize();

        ModSounds.initialize();

        ClientFlashOverlay.initialize();

        LOGGER.info("Creeper Extended (Client) initialized successfully");
    }
}
