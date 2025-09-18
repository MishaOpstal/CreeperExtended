package nl.mishaopstal.creeperextended;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.mishaopstal.creeperextended.CreeperExtendedConfig;

public class CreeperExtended implements ModInitializer {

    public static final String MOD_ID = "creeperextended";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final CreeperExtendedConfig CONFIG = CreeperExtendedConfig.createAndLoad();
    @Override
    public void onInitialize() {
        LOGGER.info("Starting Creeper Extended...");
        // Creeper behavior modifications are applied via Mixins.
    }
}
