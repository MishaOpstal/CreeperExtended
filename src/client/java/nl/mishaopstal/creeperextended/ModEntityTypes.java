package nl.mishaopstal.creeperextended;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import nl.mishaopstal.creeperextended.entity.ThrownFlashbangEntity;

public class ModEntityTypes {
    public static final RegistryKey<EntityType<?>> THROWN_FLASHBANG_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(CreeperExtended.MOD_ID, "thrown_flashbang")
    );

    public static final EntityType<ThrownFlashbangEntity> THROWN_FLASHBANG = Registry.register(
            Registries.ENTITY_TYPE,
            THROWN_FLASHBANG_KEY,
            EntityType.Builder.<ThrownFlashbangEntity>create(ThrownFlashbangEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(1)
                    .build(THROWN_FLASHBANG_KEY)
    );

    public static void initialize() {
        // Entity registration happens in the static field initialization above
        // This method is called to ensure the class is loaded and entities are registered
    }
}