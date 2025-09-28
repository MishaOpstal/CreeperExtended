package nl.mishaopstal.creeperextended.potion;

import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.item.ModItems;

public class ModPotions {
    public static final Potion STUNNED_POTION =
            Registry.register(
                    Registries.POTION,
                    Identifier.of(CreeperExtended.MOD_ID, "stun"),
                    new Potion("stun",
                            new StatusEffectInstance(
                                    Registries.STATUS_EFFECT.getEntry(CreeperExtended.STUNNED_EFFECT),
                                    160,
                                    0)));

    public static void initialize() {
        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            builder.registerPotionRecipe(
                    // Input potion.
                    Potions.WATER,
                    // Ingredient
                    ModItems.FLASHBANG,
                    // Output potion.
                    Registries.POTION.getEntry(STUNNED_POTION)
            );
        });
    }
}
