package nl.mishaopstal.creeperextended;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import nl.mishaopstal.creeperextended.effects.FlashbangStatusEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.mishaopstal.creeperextended.CreeperExtendedConfig;

public class CreeperExtended implements ModInitializer {

    public static final String MOD_ID = "creeperextended";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final CreeperExtendedConfig CONFIG = CreeperExtendedConfig.createAndLoad();
    public static final StatusEffect FLASHBANG_EFFECT = new FlashbangStatusEffect();

    /**
     * Returns the configured explosion delay in ticks (20 ticks = 1s).
     * Uses reflection to remain compatible if the generated wrapper hasn't caught up yet.
     */
    public static int getExplosionDelayTicks() {
        // Try wrapper accessor first
        try {
            var method = CONFIG.getClass().getMethod("explosionDelaySeconds");
            Object value = method.invoke(CONFIG);
            float seconds = value instanceof Number ? ((Number) value).floatValue() : 1.0f;
            return Math.max(0, Math.round(seconds * 20.0f));
        } catch (Exception ignored) {
            // Try accessing the underlying model directly
            try {
                for (var field : CONFIG.getClass().getDeclaredFields()) {
                    if (field.getType().getName().equals("nl.mishaopstal.creeperextended.CreeperExtendedConfigModel")) {
                        field.setAccessible(true);
                        Object model = field.get(CONFIG);
                        var expField = model.getClass().getDeclaredField("explosionDelaySeconds");
                        expField.setAccessible(true);
                        float seconds = expField.getFloat(model);
                        return Math.max(0, Math.round(seconds * 20.0f));
                    }
                }
            } catch (Exception ignored2) { }
            // Fallback to default defined in the model (1.0s)
            LOGGER.warn("[CreeperExtended] explosionDelaySeconds not available; defaulting to 20 ticks");
            return 20;
        }
    }

    /**
     * Returns how many extra beeps to add per completed full spin cycle.
     * Falls back to reading from the underlying model if the wrapper isn't regenerated yet.
     */
    public static int getBeepIncreasePerFullSpinCycle() {
        // Try wrapper accessor first
        try {
            var method = CONFIG.getClass().getMethod("beepAmountIncreasePerFullSpinCycle");
            Object value = method.invoke(CONFIG);
            int inc = value instanceof Number ? ((Number) value).intValue() : 0;
            return Math.max(0, inc);
        } catch (Exception ignored) {
            // Try accessing the underlying model directly
            try {
                for (var field : CONFIG.getClass().getDeclaredFields()) {
                    if (field.getType().getName().equals("nl.mishaopstal.creeperextended.CreeperExtendedConfigModel")) {
                        field.setAccessible(true);
                        Object model = field.get(CONFIG);
                        var incField = model.getClass().getDeclaredField("beepAmountIncreasePerFullSpinCycle");
                        incField.setAccessible(true);
                        int inc = incField.getInt(model);
                        return Math.max(0, inc);
                    }
                }
            } catch (Exception ignored2) { }
            // Fallback to a safe default if not available
            LOGGER.warn("[CreeperExtended] beepAmountIncreasePerFullSpinCycle not available; defaulting to 0");
            return 0;
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Creeper Extended...");

        // Initialize and register status effects
        Registry.register(Registries.STATUS_EFFECT, Identifier.of(MOD_ID, "flashbang"), FLASHBANG_EFFECT);

        // Client hooks are registered via the client entrypoint (CreeperExtendedClient).
        // Avoid touching client-only classes here, to not crash on dedicated servers.
    }
}
