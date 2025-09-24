package nl.mishaopstal.creeperextended.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import nl.mishaopstal.creeperextended.item.FlashbangItem;

public class ThrownFlashbangEntity extends PersistentProjectileEntity {
    private static final int MAX_LIFETIME = 60;
    private final ItemStack flashbangStack;

    public ThrownFlashbangEntity(EntityType<? extends ThrownFlashbangEntity> type, World world) {
        super(type, world);
        flashbangStack = new ItemStack(FlashbangItem.ITEM);
    }

    public ThrownFlashbangEntity(World world, ItemStack stack) {
        super(FlashbangItem.FLASHBANG_ENTITY_TYPE, world);
        this.flashbangStack = stack.copy();
    }

    @Override
    protected ItemStack asItemStack() {
        return flashbangStack.copy();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return flashbangStack.copy();
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte)3);
            this.discard();
        }
    }

    protected float getDragInWater() {
        return 0.99F;
    }

    @Override
    public void tick() {
        if(age > MAX_LIFETIME) {
            discard();
        }

        super.tick();
    }
}