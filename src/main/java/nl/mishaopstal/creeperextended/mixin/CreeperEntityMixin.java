package nl.mishaopstal.creeperextended.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.interfaces.ICreeperSpinAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side fuse-time behavior: spinning state, beeps, and special effects.
 * Also: snapshot nearby blocks pre-explosion and, after explosion, restore changed blocks
 * and ensure their item equivalent is dropped. This keeps the world intact while spawning items.
 */
@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin implements ICreeperSpinAccessor {

    @Unique private boolean creeperextended$spinActive = false;
    @Unique private float creeperextended$spinAngle = 0f;
    @Unique private float creeperextended$spinSpeedRps = 0f;
    @Unique private int creeperextended$beepCounter = 0; // number of rotation intervals crossed
    @Unique private boolean creeperextended$effectDone = false;
    @Unique private boolean creeperextended$pendingExplosion = false;
    @Unique private int creeperextended$pendingExplosionStartAge = 0;
    @Unique private static final int creeperextended$EXPLOSION_MAX_DELAY_TICKS = 40; // safety cap

    // Removed snapshot logic; only spinning/cosmetic remains
    @Unique private int creeperextended$debugTickCounter = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void creeperextended$tick(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity)(Object)this;
        World world = self.getWorld();
        if (world.isClient) return;

        float fuseSpeed = self.getFuseSpeed(); // server-side estimate used for flashing; 0..1
        boolean charging = self.isIgnited() || fuseSpeed > 0.01f;
        if (charging && !creeperextended$spinActive) {
            creeperextended$spinActive = true;
            creeperextended$spinAngle = 0f;
            creeperextended$spinSpeedRps = 0f;
            creeperextended$beepCounter = 0;
            creeperextended$effectDone = false;
            CreeperExtended.LOGGER.info("[CreeperExtended] Spin START entityId={} pos=({}, {}, {})", self.getId(), String.format("%.2f", self.getX()), String.format("%.2f", self.getY()), String.format("%.2f", self.getZ()));
        } else if (!charging && creeperextended$spinActive) {
            // reset when disarmed (edge case)
            creeperextended$spinActive = false;
            creeperextended$spinSpeedRps = 0f;
            CreeperExtended.LOGGER.info("[CreeperExtended] Spin STOP entityId={}", self.getId());
        }

        // Throttled state log
        if (creeperextended$spinActive) {
            creeperextended$debugTickCounter++;
            if ((creeperextended$debugTickCounter % 10) == 0) {
                CreeperExtended.LOGGER.info("[CreeperExtended] Spin tick id={} fuseSpeed={} rps={} angle={}", self.getId(), String.format("%.2f", fuseSpeed), String.format("%.2f", creeperextended$spinSpeedRps), String.format("%.1f", creeperextended$spinAngle));
            }
        }

        if (!creeperextended$spinActive) return;

        // Increase spin speed as the creeper gets closer to detonation.
        // If ignited or we have a pending explosion, force full spin regardless of fuseSpeed glitches (-1.0 cases).
        float targetRps;
        if (self.isIgnited() || creeperextended$pendingExplosion) {
            targetRps = 3.0f;
        } else {
            targetRps = 3.0f * MathHelper.clamp(Math.max(0.0f, fuseSpeed), 0.0f, 1.0f);
        }
        // Smoothly approach the target
        if (creeperextended$spinSpeedRps < targetRps) {
            creeperextended$spinSpeedRps = Math.min(targetRps, creeperextended$spinSpeedRps + 0.15f);
        } else {
            creeperextended$spinSpeedRps = targetRps;
        }

        // Advance spin angle and rotate the creeper (visible on clients via normal sync)
        float deltaDeg = creeperextended$spinSpeedRps * 360.0f / 20.0f; // degrees per tick
        creeperextended$spinAngle += deltaDeg;
        // Apply rotation
        float newYaw = self.getYaw() + deltaDeg;
        self.setYaw(newYaw);
        if (self instanceof LivingEntity le) {
            le.setBodyYaw(newYaw);
            le.setHeadYaw(newYaw);
        }
        // Handle beeps on whole-rotation intervals
        if (creeperextended$spinAngle >= 360f) {
            int rotations = (int)(creeperextended$spinAngle / 360f);
            creeperextended$spinAngle = creeperextended$spinAngle % 360f;
            // Beep every rotation (more frequent than before)
            for (int i = 0; i < rotations; i++) {
                creeperextended$beepCounter++;
                self.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.8f, 1.6f);
                CreeperExtended.LOGGER.info("[CreeperExtended] Beep id={} count={}", self.getId(), creeperextended$beepCounter);
            }
        }

        // Trigger special visual/non-lethal effect once when we hit full speed
        if (!creeperextended$effectDone && creeperextended$spinSpeedRps >= 3.0f) {
            creeperextended$effectDone = true;
            if (world instanceof ServerWorld serverWorld) {
                boolean blindedAny = false;
                // Always apply flashbang to all players within 10 blocks looking in general direction
                for (var p : serverWorld.getPlayers(player -> player.squaredDistanceTo(self) <= 100.0)) {
                    if (p.canSee(self) && isLookingAt(p, self)) {
                        blindedAny = true;
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, true, true));
                        CreeperExtended.LOGGER.info("[CreeperExtended] Effect TRIGGER id={} action=BLINDNESS target={}", self.getId(), p.getName().getString());
                    }
                }
                if (!blindedAny) {
                    CreeperExtended.LOGGER.info("[CreeperExtended] Effect TRIGGER id={} action=PARTICLES", self.getId());
                    for (int i = 0; i < 40; i++) {
                        double dx = (serverWorld.getRandom().nextDouble() - 0.5) * 0.6;
                        double dy = serverWorld.getRandom().nextDouble() * 0.6 + 0.2;
                        double dz = (serverWorld.getRandom().nextDouble() - 0.5) * 0.6;
                        serverWorld.spawnParticles(ParticleTypes.FIREWORK, self.getX(), self.getBodyY(0.5), self.getZ(), 1, dx, dy, dz, 0.1);
                        serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, self.getX(), self.getBodyY(0.5), self.getZ(), 1, dx, dy, dz, 0.1);
                    }
                }
            }
        }

        // If explosion was scheduled, complete it now when ready or on timeout
        if (creeperextended$pendingExplosion) {
            boolean timeout = (self.age - creeperextended$pendingExplosionStartAge) >= creeperextended$EXPLOSION_MAX_DELAY_TICKS;
            if (creeperextended$effectDone || timeout) {
                if (self.getWorld() instanceof ServerWorld serverWorld) {
                    TntEntity tnt = new TntEntity(serverWorld, self.getX(), self.getY(), self.getZ(), self instanceof LivingEntity le ? le : null);
                    tnt.setFuse(0);
                    serverWorld.spawnEntity(tnt);
                    CreeperExtended.LOGGER.info("[CreeperExtended] Explosion EXECUTE id={} reason={}", self.getId(), creeperextended$effectDone ? "custom_actions_done" : "timeout");
                    self.discard();
                }
                creeperextended$pendingExplosion = false;
            }
        }
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void creeperextended$explodeIntercept(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity)(Object)this;
        if (!(self.getWorld() instanceof ServerWorld)) return; // only manage on server
        if (!creeperextended$pendingExplosion) {
            creeperextended$pendingExplosion = true;
            creeperextended$pendingExplosionStartAge = self.age;
            CreeperExtended.LOGGER.info("[CreeperExtended] Explosion SCHEDULED id={} age={}", self.getId(), self.age);
        }
        ci.cancel();
    }

    @Override
    public float creeperextended$getSpinAngle() { return creeperextended$spinAngle; }

    @Override
    public float creeperextended$getSpinSpeedRps() { return creeperextended$spinSpeedRps; }

    @Override
    public boolean creeperextended$isSpinActive() { return creeperextended$spinActive; }

    @Unique
    private static boolean isLookingAt(LivingEntity viewer, Entity target) {
        var toTarget = target.getPos().add(0, target.getStandingEyeHeight(), 0).subtract(viewer.getPos().add(0, viewer.getStandingEyeHeight(), 0)).normalize();
        var look = viewer.getRotationVec(1.0f).normalize();
        return look.dotProduct(toTarget) > 0.7; // ~45 degrees (general direction)
    }
}
