package com.fromthehorror;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class Sight {
    private Sight() {}

    /** True if the player is looking at the entity and has a clear line of sight. */
    public static boolean looking(PlayerEntity p, Entity e, double threshold) {
        Vec3d look = p.getRotationVec(1.0f).normalize();
        Vec3d to = e.getEyePos().subtract(p.getEyePos()).normalize();
        return look.dotProduct(to) > threshold && p.canSee(e);
    }

    public static boolean flashlightOn(PlayerEntity p) {
        return FlashlightItem.isOn(p.getMainHandStack()) || FlashlightItem.isOn(p.getOffHandStack());
    }
}
