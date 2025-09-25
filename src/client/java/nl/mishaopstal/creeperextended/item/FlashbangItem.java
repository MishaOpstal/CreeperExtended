package nl.mishaopstal.creeperextended.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.entity.ThrownFlashbangEntity;

public class FlashbangItem extends Item {
    public FlashbangItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            CreeperExtended.LOGGER.info("Flashbang being used by {}", user.getDisplayName());

            // play sound effect
            world.playSound(null, user.getBlockPos(), SoundEvent.of(Identifier.of("creeperextended:am1_throwing_flashbang_02")), SoundCategory.PLAYERS);

            // spawn projectile using the registered entity type
            ThrownFlashbangEntity flash = new ThrownFlashbangEntity(world, user);
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