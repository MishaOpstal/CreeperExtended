package nl.mishaopstal.creeperextended.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.behaviours.CreeperExplosionBehavior;
import nl.mishaopstal.creeperextended.interfaces.ICreeperSpinAccessor;
import nl.mishaopstal.creeperextended.sound.ModSounds;
import nl.mishaopstal.creeperextended.util.FlashbangHelper;
import nl.mishaopstal.creeperextended.util.ModHelpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    @Shadow
    protected abstract void explode();

    @Unique private boolean creeperextended$spinActive = false;
    @Unique private float creeperextended$spinAngle = 0f;
    @Unique private float creeperextended$spinSpeedRps = 0f;
    @Unique private int creeperextended$beepCounter = 0; // total beeps played
    @Unique private int creeperextended$spinCyclesCompleted = 0; // completed full rotations since start
    @Unique private boolean creeperextended$pendingExplosion = false;
    @Unique private int creeperextended$pendingExplosionStartAge = 0;
    @Unique private int creeperextended$requiredExplosionDelayTicks = 0;
    @Unique private static final int creeperextended$EXPLOSION_MAX_DELAY_TICKS = 200; // safety cap (10s)

    // Removed snapshot logic; only spinning/cosmetic remains
    @Unique private int creeperextended$debugTickCounter = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void creeperextended$tick(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity)(Object)this;
        World world = self.getWorld();
        if (world.isClient) return;

        float fuseSpeed = self.getFuseSpeed(); // server-side estimate used for flashing; 0..1
        boolean charging = self.isIgnited() || fuseSpeed > 0.01f;
        boolean spinEnabled = CreeperExtended.CONFIG.spinAnimation();
        if (charging && spinEnabled && !creeperextended$spinActive) {
            creeperextended$spinActive = true;
            creeperextended$spinAngle = 0f;
            creeperextended$spinSpeedRps = 0f;
            creeperextended$beepCounter = 0;
            creeperextended$spinCyclesCompleted = 0;
            CreeperExtended.LOGGER.debug("[CreeperExtended] Spin START entityId={} pos=({}, {}, {})", self.getId(), String.format("%.2f", self.getX()), String.format("%.2f", self.getY()), String.format("%.2f", self.getZ()));
        } else if ((!charging || !spinEnabled) && creeperextended$spinActive) {
            // reset when disarmed or spin disabled
            creeperextended$spinActive = false;
            creeperextended$spinSpeedRps = 0f;
            creeperextended$spinCyclesCompleted = 0;
            CreeperExtended.LOGGER.debug("[CreeperExtended] Spin STOP entityId={}", self.getId());
        }

        // Throttled state log
        if (creeperextended$spinActive) {
            creeperextended$debugTickCounter++;
            if ((creeperextended$debugTickCounter % 10) == 0) {
                CreeperExtended.LOGGER.debug("[CreeperExtended] Spin tick id={} fuseSpeed={} rps={} angle={}", self.getId(), String.format("%.2f", fuseSpeed), String.format("%.2f", creeperextended$spinSpeedRps), String.format("%.1f", creeperextended$spinAngle));
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
            int baseBeepsPerCycle = Math.max(1, CreeperExtended.CONFIG.beepAmountPerFullSpinCycle());
            int incPerCycle = Math.max(0, CreeperExtended.getBeepIncreasePerFullSpinCycle());

            int rotations = (int)(creeperextended$spinAngle / (360f *baseBeepsPerCycle)); // handle multiple rotations
            if (rotations < 1) rotations = 1; // always at least one
            creeperextended$spinAngle = creeperextended$spinAngle % 360f;
            boolean beeping = CreeperExtended.CONFIG.beeping();

            float volume = CreeperExtended.CONFIG.beepVolume();
            if (beeping) {
                for (int r = 0; r < rotations; r++) {
                    int beepsThisCycle = baseBeepsPerCycle + (creeperextended$spinCyclesCompleted * incPerCycle);
                    for (int i = 0; i < beepsThisCycle; i++) {
                        creeperextended$beepCounter++;
                        self.playSound(ModSounds.BOMB_BEEP, volume, 1.0f);
                    }
                    CreeperExtended.LOGGER.debug("[CreeperExtended] BeepCycle id={} cycle={} beepsThisCycle={} totalBeeps={} vol={} base={} inc={}"
                            , self.getId(), creeperextended$spinCyclesCompleted, beepsThisCycle, creeperextended$beepCounter, String.format("%.2f", volume), baseBeepsPerCycle, incPerCycle);
                    creeperextended$spinCyclesCompleted++;
                }
            }
        }


        // If explosion was scheduled, complete it now when ready or on timeout
        if (creeperextended$pendingExplosion) {
            int elapsed = self.age - creeperextended$pendingExplosionStartAge;
            boolean timeout = elapsed >= creeperextended$EXPLOSION_MAX_DELAY_TICKS;
            boolean delayElapsed = elapsed >= creeperextended$requiredExplosionDelayTicks;
            boolean ready = delayElapsed || timeout;
            if (ready) {
                if (self.getWorld() instanceof ServerWorld serverWorld) {
                    // Apply flashbang-style effects at the moment of explosion
                    boolean blindedAny = false;

                    boolean doFlash = CreeperExtended.CONFIG.flashbangEnabled();
                    boolean doCreeperFlash = CreeperExtended.CONFIG.creeperFlashbang();
                    int radius = CreeperExtended.CONFIG.flashbangRadius();
                    int duration = CreeperExtended.CONFIG.flashbangHoldTicks();
                    double radiusSq = (double)radius * (double)radius;

                    if (doFlash && doCreeperFlash) {
                        int fadeInTicks = MathHelper.clamp(CreeperExtended.getFlashbangFadeInTicks(), 0, 127);
                        int fadeOutTicks = CreeperExtended.getFlashbangFadeOutTicks();
                        int totalDuration = duration + fadeOutTicks + fadeInTicks;

                        for (var p : serverWorld.getPlayers(player -> player.squaredDistanceTo(self) <= radiusSq)) {
                            if (p.canSee(self) && ModHelpers.isLookingAt(p, self)) {
                                blindedAny = true;

                                // Apply the effect
                                FlashbangHelper.applyFlashEffect(p, totalDuration, fadeInTicks);
                                FlashbangHelper.playExplosionSound(serverWorld, self.getBlockPos());
                                CreeperExtended.LOGGER.debug("[CreeperExtended] Effect TRIGGER id={} action=FLASHBANG target={} r={} d={}", self.getId(), p.getName().getString(), radius, totalDuration);
                            }
                        }

                        for (var mob : serverWorld.getEntitiesByClass(LivingEntity.class, self.getBoundingBox().expand(radius),
                                entity -> !(entity instanceof ServerPlayerEntity) && entity.isAlive())) {
                            if (ModHelpers.isLookingAt(mob, self)) {
                                // Slowness effect duration is shorter for mobs
                                int mobDuration = MathHelper.clamp(totalDuration / 2, 1, 127);
                                FlashbangHelper.applySlownessEffect(mob, mobDuration, 1);
                            }
                        }
                    }

                    if (!blindedAny && CreeperExtended.CONFIG.showParticles()) {
                        CreeperExtended.LOGGER.debug("[CreeperExtended] Effect TRIGGER id={} action=PARTICLES", self.getId());
                        for (int i = 0; i < 40; i++) {
                            double dx = (serverWorld.getRandom().nextDouble() - 0.5) * 0.6;
                            double dy = serverWorld.getRandom().nextDouble() * 0.6 + 0.2;
                            double dz = (serverWorld.getRandom().nextDouble() - 0.5) * 0.6;
                            serverWorld.spawnParticles(ParticleTypes.FIREWORK, self.getX(), self.getBodyY(0.5), self.getZ(), 1, dx, dy, dz, 0.1);
                            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, self.getX(), self.getBodyY(0.5), self.getZ(), 1, dx, dy, dz, 0.1);
                        }
                    }

                    DynamicRegistryManager dynamicRegistryManager = serverWorld.getRegistryManager();
                    DamageSource damageSource = new DamageSources(dynamicRegistryManager).explosion(self, self instanceof LivingEntity le ? le : null);
                    CreeperExplosionBehavior creeperExplosionBehavior = new CreeperExplosionBehavior(self);

                    // Determine whether this is a charged creeper
                    creeperExplosionBehavior.setCharged(self.isCharged());

                    // Create the explosion
                    serverWorld.createExplosion(
                            self,
                            damageSource,
                            creeperExplosionBehavior,
                            self.getX(),
                            self.getY(),
                            self.getZ(),
                            creeperExplosionBehavior.getExplosionPower(),
                            creeperExplosionBehavior.getExplosionCausesFire(),
                            ServerWorld.ExplosionSourceType.TNT
                    );
                    String reason = timeout ? "timeout" : "delay";
                    CreeperExtended.LOGGER.debug("[CreeperExtended] Explosion EXECUTE id={} reason={} elapsed={} requiredDelay={}", self.getId(), reason, elapsed, creeperextended$requiredExplosionDelayTicks);
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
            int delayTicks = Math.max(0, CreeperExtended.getExplosionDelayTicks());
            // Clamp to safety cap
            creeperextended$requiredExplosionDelayTicks = Math.min(delayTicks, creeperextended$EXPLOSION_MAX_DELAY_TICKS);
            CreeperExtended.LOGGER.debug("[CreeperExtended] Explosion SCHEDULED id={} age={} delayTicks={} (cap={})", self.getId(), self.age, creeperextended$requiredExplosionDelayTicks, creeperextended$EXPLOSION_MAX_DELAY_TICKS);
        }
        ci.cancel();
    }

    @Override
    public float creeperextended$getSpinAngle() { return creeperextended$spinAngle; }

    @Override
    public float creeperextended$getSpinSpeedRps() { return creeperextended$spinSpeedRps; }

    @Override
    public boolean creeperextended$isSpinActive() { return creeperextended$spinActive; }
}
