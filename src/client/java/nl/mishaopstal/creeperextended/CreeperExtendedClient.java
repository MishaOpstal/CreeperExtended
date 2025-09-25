package nl.mishaopstal.creeperextended;

import net.fabricmc.api.ClientModInitializer;
import nl.mishaopstal.creeperextended.client.ClientFlashOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreeperExtendedClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CreeperExtended.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Creeper Extended (Client)");

        // Register client overlays and callbacks
        ClientFlashOverlay.initialize();
        ModEntityTypes.initialize();
        ModItems.initialize();

        LOGGER.info("Creeper Extended (Client) initialized successfully");
    }
}
