package nl.mishaopstal.creeperextended.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class StunnedStatusEffect extends StatusEffect {

    public StunnedStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0xAAAAAA); // gray color
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // Return true so applyUpdateEffect runs every tick
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        // Cancel horizontal movement
        entity.setVelocity(0, entity.getVelocity().y, 0);
        entity.velocityModified = true;

        // Stop jumping
        entity.setJumping(false);

        // Stop mob pathfinding
        entity.setVelocity(0, 0, 0);
        entity.setMovementSpeed(0);
        entity.setMovement(false, new Vec3d(0,0,0));

        // Stop attacking
        if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        // Return true to indicate we "applied" something
        return true;
    }
}