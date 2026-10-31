package com.fromthehorror;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Stalks you through caves. Freezes while watched; a flashlight stare scares it off. */
public class DwellerEntity extends HostileEntity {
    private int life = 20 * 150;
    private int stare = 0;
    private boolean screamed = false;

    public DwellerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.38)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    @Override
    public void tick() {
        super.tick();
        World w = this.getWorld();
        if (w.isClient) return;
        if (--life <= 0) { vanish(); return; }

        PlayerEntity p = w.getClosestPlayer(this, 64.0);
        if (p == null || p.isSpectator()) {
            if (this.age > 600) vanish();
            return;
        }
        boolean seen = Sight.looking(p, this, 0.96);
        double dist = this.distanceTo(p);

        if (seen) {
            this.getNavigation().stop();
            this.getLookControl().lookAt(p, 30f, 30f);
            if (Sight.flashlightOn(p)) {
                this.setGlowing(true);
                if (++stare > 80) { vanish(); return; }
            } else {
                this.setGlowing(false);
                stare = Math.max(0, stare - 1);
            }
            if (!screamed && dist < 14) {
                screamed = true;
                w.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, 1.0f, 0.6f);
            }
        } else {
            this.setGlowing(false);
            this.getNavigation().startMovingTo(p, 1.5);
            if (dist < 2.2 && this.age % 20 == 0) this.tryAttack(p);
        }
        if (this.age % 40 == 0 && dist < 30) {
            w.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

    private void vanish() {
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        }
        this.discard();
    }
}
