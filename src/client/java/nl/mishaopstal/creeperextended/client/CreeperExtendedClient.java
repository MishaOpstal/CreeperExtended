package nl.mishaopstal.creeperextended.client;

import net.fabricmc.api.ClientModInitializer;

public class CreeperExtendedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register client overlays and callbacks
        ClientFlashOverlay.init();
    }
}
