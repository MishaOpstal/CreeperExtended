package nl.mishaopstal.creeperextended.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.entity.ThrownFlashbangEntity;

public class FlashbangItem extends Item {
    static Identifier id = Identifier.of(CreeperExtended.MOD_ID, "flashbang");
    static RegistryKey<EntityType<?>> entityKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id);

    // Create a RegistryKey for the item
    static RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);

    public static EntityType<ThrownFlashbangEntity> FLASHBANG_ENTITY_TYPE = EntityType.Builder
            .<ThrownFlashbangEntity>create(ThrownFlashbangEntity::new, SpawnGroup.MISC)
            .dimensions(0.25F, 0.25F)
            .maxTrackingRange(4)
            .trackingTickInterval(10)
            .build(entityKey);

    // Register the item with the registryKey set in Settings
    public static final Item ITEM = Registry.register(
            Registries.ITEM,
            itemKey,  // Use the RegistryKey here instead of Identifier
            new FlashbangItem(new Item.Settings()
                    .registryKey(itemKey))  // Set the registry key in settings
    );

    public FlashbangItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            // spawn projectile
            ThrownFlashbangEntity flash = new ThrownFlashbangEntity(
                    FLASHBANG_ENTITY_TYPE, world);
            flash.setOwner(user);
            flash.setPosition(user.getX(), user.getEyeY() - 0.1, user.getZ());
            flash.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            world.spawnEntity(flash);

            if (!user.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }

        return ActionResult.SUCCESS;
    }
}