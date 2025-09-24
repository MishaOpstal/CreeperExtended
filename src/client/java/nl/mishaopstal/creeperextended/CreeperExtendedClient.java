package nl.mishaopstal.creeperextended;

import net.fabricmc.api.ClientModInitializer;
import nl.mishaopstal.creeperextended.client.ClientFlashOverlay;

public class CreeperExtendedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register client overlays and callbacks
        ClientFlashOverlay.initialize();
        ModItems.initialize();
    }
}
