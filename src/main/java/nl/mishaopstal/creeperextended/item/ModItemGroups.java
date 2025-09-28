package nl.mishaopstal.creeperextended.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import nl.mishaopstal.creeperextended.CreeperExtended;

public class ModItemGroups {
    public static final ItemGroup CREEPER_EXTENDED_ITEM_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(CreeperExtended.MOD_ID, "creeper_extended_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.FLASHBANG))
                    .displayName(Text.translatable("item.group.creeperextended.items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.FLASHBANG);
                        entries.add(ModItems.SEMBER_MUSIC_DISC);

                    }).build());


    public static void initialize() {
        CreeperExtended.LOGGER.info("Registering Item Groups");
    }
}