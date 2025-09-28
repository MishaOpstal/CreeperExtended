package nl.mishaopstal.creeperextended.behaviours;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.EntityExplosionBehavior;
import net.minecraft.world.explosion.Explosion;

import java.util.Optional;

public class CreeperExplosionBehavior extends EntityExplosionBehavior {
    private final Entity entity;
    private boolean isCharged;

    public CreeperExplosionBehavior(Entity entity) {
        super(entity);
        this.entity = entity;
    }

    @Override
    public Optional<Float> getBlastResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState) {
        return super.getBlastResistance(explosion, world, pos, blockState, fluidState).map((max) -> this.entity.getEffectiveExplosionResistance(explosion, world, pos, blockState, fluidState, max));
    }

    public float getExplosionPower() {
        return isCharged ? 6.0f : 3.0f;
    }

    public boolean getExplosionCausesFire() {
        // Get the world
        ServerWorld world = (ServerWorld) entity.getWorld();

        // Check if the creeper is charged
        if (!isCharged()) {
            return false;
        }

        // Check if in swamp biome
        BlockPos entityPos = entity.getBlockPos();
        String biomeId = world.getBiome(entityPos).getKey().map(key -> key.getValue().toString()).orElse("");
        if (biomeId.contains("swamp") || biomeId.contains("mangrove")) {
            return false;
        }

        // Check if underwater
        if (this.entity.isTouchingWaterOrRain()) {
            return false;
        }

        // Default behavior
        return true;
    }

    public boolean isCharged() {
        return isCharged;
    }

    public void setCharged(boolean charged) {
        isCharged = charged;
    }
}
