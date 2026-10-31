package com.fromthehorror;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Schedules random scares per player: darkness fog, footsteps, whispers, and spawns. */
public final class DreadDirector {
    private DreadDirector() {}

    private static final Random R = new Random();
    private static final Map<UUID, Integer> TIMERS = new HashMap<>();
    private record Pending(ServerWorld w, double x, double y, double z, SoundEvent s, float pitch, long at) {}
    private static final List<Pending> PENDING = new ArrayList<>();

    private static int nextDelay() { return 90 + R.nextInt(210); } // seconds

    public static void tick(ServerWorld world) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;
        long now = world.getTime();

        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.w() == world && now >= p.at()) {
                world.playSound(null, p.x(), p.y(), p.z(), p.s(), SoundCategory.AMBIENT, 1.0f, p.pitch());
                it.remove();
            }
        }

        if (now % 20 != 0) return;
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.isSpectator()) continue;
            int t = TIMERS.getOrDefault(p.getUuid(), nextDelay()) - 1;
            if (t > 0) { TIMERS.put(p.getUuid(), t); continue; }
            TIMERS.put(p.getUuid(), nextDelay());
            trigger(world, p);
        }
    }

    public static void trigger(ServerWorld w, ServerPlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        boolean underground = !w.isSkyVisible(pos) && pos.getY() < 56;
        boolean night = w.isNight();
        int roll = R.nextInt(100);
        if (underground) {
            if (roll < 30) spawn(w, p, FromTheHorror.DWELLER, 26, 34);
            else if (roll < 55) fog(p);
            else if (roll < 80) footsteps(w, p);
            else whisper(p);
        } else if (night) {
            if (roll < 30) spawn(w, p, FromTheHorror.WATCHER, 34, 50);
            else if (roll < 60) fog(p);
            else if (roll < 80) footsteps(w, p);
            else whisper(p);
        } else if (roll < 15) {
            whisper(p);
        }
    }

    /** ServerPlayerEntity#playSound excludes that player on the server, so go through the world instead. */
    private static void play(ServerPlayerEntity p, SoundEvent s, float vol, float pitch) {
        p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundCategory.AMBIENT, vol, pitch);
    }

    /** Drop all scheduled state (server stop / player leave) so nothing leaks between worlds. */
    public static void clear() {
        PENDING.clear();
        TIMERS.clear();
    }

    public static void forget(UUID id) { TIMERS.remove(id); }

    private static void fog(ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 20 * 25, 0, false, false, false));
        play(p, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSE, 1.0f, 0.6f);
    }

    private static void whisper(ServerPlayerEntity p) {
        play(p, SoundEvents.ENTITY_VEX_AMBIENT, 0.8f, 0.5f);
    }

    private static void footsteps(ServerWorld w, ServerPlayerEntity p) {
        Vec3d back = p.getRotationVec(1.0f).multiply(-1, 0, -1).normalize().multiply(5);
        long now = w.getTime();
        for (int i = 0; i < 4; i++) {
            PENDING.add(new Pending(w, p.getX() + back.x, p.getY(), p.getZ() + back.z,
                    SoundEvents.BLOCK_STONE_STEP, 0.7f, now + i * 9L));
        }
    }

    public static boolean spawn(ServerWorld w, ServerPlayerEntity p, EntityType<? extends HostileEntity> type, int min, int max) {
        boolean exists = !w.getEntitiesByClass(HostileEntity.class, p.getBoundingBox().expand(96), e -> e.getType() == type).isEmpty();
        if (exists) return false;
        for (int i = 0; i < 12; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            double d = min + R.nextInt(max - min + 1);
            int x = (int) (p.getX() + Math.cos(ang) * d);
            int z = (int) (p.getZ() + Math.sin(ang) * d);
            if (!w.isChunkLoaded(new BlockPos(x, 0, z))) continue;
            for (int y = p.getBlockY() + 6; y >= p.getBlockY() - 10; y--) {
                BlockPos at = new BlockPos(x, y, z);
                if (w.getBlockState(at).isAir() && w.getBlockState(at.up()).isAir() && w.getBlockState(at.up(2)).isAir()
                        && w.getBlockState(at.down()).isSolidBlock(w, at.down())) {
                    HostileEntity e = type.create(w);
                    if (e == null) return false;
                    e.refreshPositionAndAngles(x + 0.5, y, z + 0.5, R.nextFloat() * 360f, 0f);
                    w.spawnEntity(e);
                    return true;
                }
            }
        }
        return false;
    }
}
