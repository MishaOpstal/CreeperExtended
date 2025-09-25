package nl.mishaopstal.creeperextended.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.ModEntityTypes;
import org.spongepowered.asm.mixin.Unique;

public class ThrownFlashbangEntity extends ThrownItemEntity {
    private static final double BOUNCE_COEFFICIENT = 0.6; // energy retained on the impacted axis
    private static final double AIR_DRAG = 0.99;          // slow down while airborne
    private static final double GROUND_FRICTION = 0.75;   // horizontal friction while sliding on ground
    private static final double REST_VELOCITY_SQR = 0.0016; // ~0.04^2
    private static final int DESPAWN_AFTER_REST_TICKS = 80; // 4 seconds

    private static final int MAX_LIFETIME_TICKS = 120; // 6 seconds @ 20 TPS
    private static final int STILL_X_RANGE = 6;
    private static final int STILL_Y_RANGE = 3;
    private static final int STILL_TICKS_REQUIRED = 20; // e.g. 1 second of stability

    private int lifetimeTicks = 0;
    private int stableTicks = 0;
    private double refX = Double.NaN;
    private double refY = Double.NaN;

    public ThrownFlashbangEntity(EntityType<? extends ThrownFlashbangEntity> entityType, World world) {
        super(entityType, world);
    }

    public ThrownFlashbangEntity(World world, LivingEntity owner) {
        super(ModEntityTypes.THROWN_FLASHBANG, owner, world, getFlashbangItemStack());
    }

    public ThrownFlashbangEntity(World world, double x, double y, double z) {
        super(ModEntityTypes.THROWN_FLASHBANG, x, y, z, world, getFlashbangItemStack());
    }

    private static ItemStack getFlashbangItemStack() {
        // Get the flashbang item from the registry to avoid circular dependency
        Item flashbangItem = Registries.ITEM.get(Identifier.of(CreeperExtended.MOD_ID, "flashbang"));
        return new ItemStack(flashbangItem);
    }

    @Override
    protected Item getDefaultItem() {
        return Registries.ITEM.get(Identifier.of(CreeperExtended.MOD_ID, "flashbang"));
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        // Do not call super.onCollision() here; it would discard the entity for thrown items.
        if (this.getWorld().isClient) return;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hitResult;
            Direction side = bhr.getSide();
            Vec3d v = this.getVelocity();
            double vx = v.x;
            double vy = v.y;
            double vz = v.z;

            switch (side.getAxis()) {
                case X -> { // hit a face along X, invert X velocity and damp tangential
                    vx = -vx * BOUNCE_COEFFICIENT;
                    vy *= 0.7;
                    vz *= 0.7;
                }
                case Y -> { // floor/ceiling
                    vy = -vy * BOUNCE_COEFFICIENT;
                    vx *= 0.7;
                    vz *= 0.7;
                }
                case Z -> { // hit a face along Z
                    vz = -vz * BOUNCE_COEFFICIENT;
                    vx *= 0.7;
                    vy *= 0.7;
                }
            }

            this.setVelocity(vx, vy, vz);
            // Nudge out of the block along the normal to avoid sticking
            this.setPosition(this.getX() + side.getOffsetX() * 0.001,
                    this.getY() + side.getOffsetY() * 0.001,
                    this.getZ() + side.getOffsetZ() * 0.001);
            this.fallDistance = 0.0F;

            // Update reference anchor to current position
            this.refX = this.getX();
            this.refY = this.getY();
            this.stableTicks = 0; // reset stability counter
        } else if (hitResult.getType() == HitResult.Type.ENTITY) {
            // Lightly bounce back from entities
            if (hitResult instanceof EntityHitResult) {
                Vec3d v = this.getVelocity();
                this.setVelocity(v.multiply(-0.5));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            lifetimeTicks++;
            if (lifetimeTicks >= MAX_LIFETIME_TICKS) {
                this.discard();
                return;
            }

            if (!Double.isNaN(refX) && !Double.isNaN(refY)) {
                // Check if we're within stable bounds relative to last collision point
                boolean withinX = Math.abs(this.getX() - refX) <= STILL_X_RANGE;
                boolean withinY = Math.abs(this.getY() - refY) <= STILL_Y_RANGE;

                if (withinX && withinY) {
                    stableTicks++;
                } else {
                    // reset if wandered too far
                    stableTicks = 0;
                    refX = this.getX();
                    refY = this.getY();
                }

                if (stableTicks >= STILL_TICKS_REQUIRED) {
                    CreeperExtended.LOGGER.info("Flashbang is resting.");
                    boolean doFlash = CreeperExtended.CONFIG.flashbangEnabled();

                    int fadeInTicks = MathHelper.clamp(CreeperExtended.getFlashbangFadeInTicks(), 0, 127);
                    int fadeOutTicks = CreeperExtended.getFlashbangFadeOutTicks();
                    int radius = CreeperExtended.CONFIG.flashbangRadius();
                    int duration = CreeperExtended.CONFIG.flashbangHoldTicks();
                    double radiusSq = (double)radius * (double)radius;
                    int totalDuration = duration + fadeOutTicks + fadeInTicks;

                    if (getWorld() instanceof ServerWorld serverWorld) {
                        if (doFlash) {
                            for (var p : serverWorld.getPlayers(player -> player.squaredDistanceTo(this) <= radiusSq)) {
                                if (eyePathToTargetClear(p, this) && isLookingAt(p, this)) {
                                    p.addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(CreeperExtended.FLASHBANG_EFFECT), totalDuration, fadeInTicks, false, false, false));
                                    CreeperExtended.LOGGER.debug("[CreeperExtended] Effect TRIGGER id={} action=FLASHBANG target={} r={} d={}", this.getId(), p.getName().getString(), radius, totalDuration);
                                }
                            }
                        }
                    }

                    this.discard();
                }
            }
        }
    }

    @Override
    protected double getGravity() {
        return 0.03F;
    }

    @Unique
    private static boolean isLookingAt(LivingEntity viewer, Entity target) {
        var toTarget = target.getPos().add(0, target.getStandingEyeHeight(), 0).subtract(viewer.getPos().add(0, viewer.getStandingEyeHeight(), 0)).normalize();
        var look = viewer.getRotationVec(1.0f).normalize();
        return look.dotProduct(toTarget) > 0.7; // ~45 degrees (general direction)
    }

    private static boolean eyePathToTargetClear(LivingEntity viewer, Entity target) {
        var from = viewer.getCameraPosVec(1.0f);
        var to = target.getPos().add(0, target.getStandingEyeHeight(), 0);
        var world = viewer.getWorld();
        var ray = world.raycast(new net.minecraft.world.RaycastContext(from, to, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, viewer));
        return ray.getType() == HitResult.Type.MISS;
    }
}