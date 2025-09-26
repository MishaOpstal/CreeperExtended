package nl.mishaopstal.creeperextended.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

public class ModHelpers {
    private static final float DEFAULT_VOLUME = 1.0f;
    private static final float DEFAULT_PITCH = 1.0f;

    // AUDIO HELPERS
    public static void playSound(World world, BlockPos pos, SoundEvent sound, SoundCategory soundCategory, float volume, float pitch) {
        world.playSound(null, pos, sound, soundCategory, volume, pitch);
    }

    public static void playSound(World world, BlockPos pos, SoundEvent sound, SoundCategory soundCategory) {
        world.playSound(null, pos, sound, soundCategory, DEFAULT_VOLUME, DEFAULT_PITCH);
    }

    public static void playSound(World world, BlockPos pos, String soundIdentifier, SoundCategory soundCategory, float volume, float pitch) {
        playSound(world, pos, SoundEvent.of(Identifier.of(soundIdentifier)), soundCategory, volume, pitch);
    }

    public static void playSound(World world, BlockPos pos, String soundIdentifier, SoundCategory soundCategory) {
        playSound(world, pos, SoundEvent.of(Identifier.of(soundIdentifier)), soundCategory);
    }

    // STATUS EFFECT HELPERS
    public static void applyStatusEffect(LivingEntity entity,
                                         RegistryEntry<StatusEffect> effectEntry,
                                         int duration,
                                         int amplifier) {

        StatusEffectInstance inst = new StatusEffectInstance(
                effectEntry,
                duration,
                amplifier,
                false,  // ambient?
                true,   // show particles?
                true    // show icon?
        );
        entity.addStatusEffect(inst);
    }

    // LINE-OF-SIGHT HELPERS
    @Unique
    public static boolean isLookingAt(LivingEntity viewer, Entity target) {
        var toTarget = target.getPos().add(0, target.getStandingEyeHeight(), 0).subtract(viewer.getPos().add(0, viewer.getStandingEyeHeight(), 0)).normalize();
        var look = viewer.getRotationVec(1.0f).normalize();
        return look.dotProduct(toTarget) > 0.65; // ~49 degrees (a bit more lenient)
    }

    @Unique
    public static boolean eyePathToTargetClear(LivingEntity viewer, Entity target) {
        // Robust line-of-sight that tolerates the flashbang clipping slightly into blocks
        // but still prevents triggering through real walls.
        var world = viewer.getWorld();

        var from = viewer.getCameraPosVec(1.0f);
        var toCenter = target.getPos().add(0, target.getStandingEyeHeight(), 0);
        var dir = toCenter.subtract(from).normalize();

        // Back the endpoint off slightly so rays don't terminate inside a block the grenade touches
        var toBacked = toCenter.subtract(dir.multiply(0.25)); // 25 cm back toward viewer

        var ray1 = world.raycast(new net.minecraft.world.RaycastContext(
                from, toBacked,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                viewer));

        if (ray1.getType() == HitResult.Type.MISS) {
            return true;
        }

        // If we hit a block, accept if the hit is in the same block as the grenade (endpoint grazing)
        if (ray1 instanceof BlockHitResult bhr1) {
            if (bhr1.getBlockPos().equals(target.getBlockPos())) {
                return true;
            }
            // Or if the hit is extremely close to the endpoint (floating-point/voxel edge cases)
            var hitPos = Vec3d.ofCenter(bhr1.getBlockPos());
            if (hitPos.squaredDistanceTo(toCenter) < 0.06) { // within ~24.5 cm
                return true;
            }
        }

        // Second attempt: nudge the endpoint slightly upward then back off again.
        var toUp = toCenter.add(0, 0.2, 0).subtract(dir.multiply(0.25));
        var ray2 = world.raycast(new net.minecraft.world.RaycastContext(
                from, toUp,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                viewer));

        if (ray2.getType() == HitResult.Type.MISS) {
            return true;
        }
        if (ray2 instanceof BlockHitResult bhr2) {
            return bhr2.getBlockPos().equals(target.getBlockPos());
        }

        return false;
    }
}
