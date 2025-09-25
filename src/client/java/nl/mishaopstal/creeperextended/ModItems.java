package nl.mishaopstal.creeperextended;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import nl.mishaopstal.creeperextended.item.FlashbangItem;

public class ModItems {
    // Register the flashbang item first
    public static final Item FLASHBANG_ITEM = Registry.register(
            Registries.ITEM,
            Identifier.of(CreeperExtended.MOD_ID, "flashbang"),
            new FlashbangItem(new Item.Settings())
    );

    // Create a registry key for the item group
    public static final RegistryKey<ItemGroup> CUSTOM_ITEM_GROUP_KEY =
            RegistryKey.of(Registries.ITEM_GROUP.getKey(),
                    Identifier.of(CreeperExtended.MOD_ID, "creeper_extended"));

    // Defer item group creation to avoid the "Item id not set" error
    public static ItemGroup CUSTOM_ITEM_GROUP;

    public static void initialize() {
        // Create and register the item group during initialization
        CUSTOM_ITEM_GROUP = FabricItemGroup.builder()
                .icon(() -> new ItemStack(FLASHBANG_ITEM))
                .displayName(Text.translatable("item.group.creeperextended"))
                .build();

        Registry.register(Registries.ITEM_GROUP, CUSTOM_ITEM_GROUP_KEY, CUSTOM_ITEM_GROUP);

        // Add items to the group
        ItemGroupEvents.modifyEntriesEvent(CUSTOM_ITEM_GROUP_KEY)
                .register(entries -> entries.add(FLASHBANG_ITEM));
    }
}