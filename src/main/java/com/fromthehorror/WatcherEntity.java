package com.fromthehorror;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** A still figure at the edge of your vision. Vanishes if you stare, approach, or hit it. */
public class WatcherEntity extends HostileEntity {
    private int life = 20 * 90;
    private int stared = 0;

    public WatcherEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 100.0);
    }

    @Override
    public void tick() {
        super.tick();
        World w = this.getWorld();
        this.bodyYaw = this.headYaw;
        if (w.isClient) return;
        if (--life <= 0) { vanish(false); return; }
        PlayerEntity p = w.getClosestPlayer(this, 100.0);
        if (p == null || p.isSpectator()) return;
        this.getLookControl().lookAt(p, 30f, 30f);
        if (Sight.looking(p, this, 0.985)) stared++; else stared = Math.max(0, stared - 2);
        if (stared > 50 || this.distanceTo(p) < 9) vanish(true);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.getWorld().isClient) vanish(true);
        return false;
    }

    @Override
    public boolean isPushable() { return false; }

    private void vanish(boolean sound) {
        if (sound) {
            this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, 0.7f, 0.5f);
        }
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 25, 0.3, 0.8, 0.3, 0.01);
        }
        this.discard();
    }
}
