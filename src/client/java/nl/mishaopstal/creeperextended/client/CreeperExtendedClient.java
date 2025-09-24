package nl.mishaopstal.creeperextended.client;

import net.fabricmc.api.ClientModInitializer;
import nl.mishaopstal.creeperextended.ModItems;

public class CreeperExtendedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register client overlays and callbacks
        ClientFlashOverlay.initialize();
        ModItems.initialize();
    }
}
