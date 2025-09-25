package nl.mishaopstal.creeperextended.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.CreeperExtended;
import nl.mishaopstal.creeperextended.ModEntityTypes;

public class ThrownFlashbangEntity extends ThrownItemEntity {
    private static final int MAX_LIFETIME = 60; // 3 seconds at 20 TPS

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
        // Get the flashbang item from registry to avoid circular dependency
        Item flashbangItem = Registries.ITEM.get(Identifier.of(CreeperExtended.MOD_ID, "flashbang"));
        return new ItemStack(flashbangItem);
    }

    @Override
    protected Item getDefaultItem() {
        return Registries.ITEM.get(Identifier.of(CreeperExtended.MOD_ID, "flashbang"));
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        if (!this.getWorld().isClient) {
            // TODO: Add flashbang explosion effects here
            this.getWorld().sendEntityStatus(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Auto-discard after max lifetime
        if (this.age > MAX_LIFETIME) {
            if (!this.getWorld().isClient) {
                // TODO: Add flashbang explosion effects here
                this.getWorld().sendEntityStatus(this, (byte) 3);
                this.discard();
            }
        }
    }

    @Override
    protected double getGravity() {
        return 0.03F;
    }
}