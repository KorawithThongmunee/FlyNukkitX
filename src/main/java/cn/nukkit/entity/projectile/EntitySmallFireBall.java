package cn.nukkit.entity.projectile;

import cn.nukkit.api.PowerNukkitOnly;
import cn.nukkit.api.PowerNukkitXOnly;
import cn.nukkit.api.Since;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockFire;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.block.BlockIgniteEvent;
import cn.nukkit.event.entity.*;
import cn.nukkit.level.MovingObjectPosition;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.vibration.VibrationEvent;
import cn.nukkit.level.vibration.VibrationType;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;

@PowerNukkitXOnly
@Since("1.19.60-r1")
public class EntitySmallFireBall extends EntityProjectile {

    public static final int NETWORK_ID = 94;

    public EntitySmallFireBall(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getHeight() {
        return 0.3125f;
    }

    @Override
    public float getWidth() {
        return 0.3125f;
    }

    @Override
    public float getLength() {
        return 0.3125f;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.age > 1200 || this.isCollided) {
            this.kill();
            hasUpdate = true;
        }

        this.timing.stopTiming();

        return hasUpdate;
    }

    @Override
    public int getResultDamage() {
        return 6;
    }

    public void onCollideWithEntity(Entity entity) {
        ProjectileHitEvent projectileHitEvent = new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity));
        this.server.getPluginManager().callEvent(projectileHitEvent);
        if (projectileHitEvent.isCancelled()) return;
        this.level.getVibrationManager().callVibrationEvent(new VibrationEvent(this, this.clone(), VibrationType.PROJECTILE_LAND));
        var damage = this.getResultDamage(entity);
        EntityDamageEvent ev = new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
        if (entity.attack(ev)) {
            addHitEffect();
            this.hadCollision = true;
            EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this, entity, 5);
            this.server.getPluginManager().callEvent(event);
            if (!event.isCancelled())
                entity.setOnFire(event.getDuration());
        }
        afterCollisionWithEntity(entity);
        this.close();
    }

    @Since("1.4.0.0-PN")
    @PowerNukkitOnly
    @Override
    protected void onCollideWithBlock(Position position, Vector3 motion) {
        this.level.getVibrationManager().callVibrationEvent(new VibrationEvent(this, this.clone(), VibrationType.PROJECTILE_LAND));
        var affect = false;
        for (Block collisionBlock : level.getCollisionBlocks(getBoundingBox().grow(0.1, 0.1, 0.1)))
            affect = onCollideWithBlock(position, motion, collisionBlock);
        if (!affect && this.getLevelBlock().getId() == BlockID.AIR) {
            BlockFire fire = (BlockFire) Block.get(BlockID.FIRE);
            fire.x = this.x;
            fire.y = this.y;
            fire.z = this.z;
            fire.level = level;

            if (fire.isBlockTopFacingSurfaceSolid(fire.down()) || fire.canNeighborBurn()) {
                BlockIgniteEvent e = new BlockIgniteEvent(this.getLevelBlock(), null, null, BlockIgniteEvent.BlockIgniteCause.FIREBALL);
                level.getServer().getPluginManager().callEvent(e);
                if (!e.isCancelled()) {
                    level.setBlock(fire, fire, true);
                }
            }
        }
    }

    @Override
    public String getOriginalName() {
        return "Small FireBall";
    }
}