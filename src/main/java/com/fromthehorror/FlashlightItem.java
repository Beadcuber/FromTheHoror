package com.fromthehorror;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FlashlightItem extends Item {
    public FlashlightItem(Settings settings) { super(settings); }

    public static boolean isOn(ItemStack s) {
        return s.getItem() instanceof FlashlightItem && s.hasNbt() && s.getNbt().getBoolean("On");
    }

    private static boolean dead(ItemStack s) { return s.getDamage() >= s.getMaxDamage() - 1; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack s = player.getStackInHand(hand);
        if (dead(s)) return TypedActionResult.fail(s);
        boolean on = !isOn(s);
        s.getOrCreateNbt().putBoolean("On", on);
        if (!on && !world.isClient) {
            StatusEffectInstance nv = player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (nv != null && nv.isAmbient() && nv.getDuration() <= 260) player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.6f, on ? 1.4f : 1.0f);
        return TypedActionResult.success(s, world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof PlayerEntity p) || !isOn(stack)) return;
        boolean held = selected || p.getOffHandStack() == stack;
        if (!held || dead(stack)) {
            stack.getOrCreateNbt().putBoolean("On", false);
            return;
        }
        if (world.getTime() % 20 == 0) stack.setDamage(stack.getDamage() + 1); // 1 battery unit per second
        if (!p.hasStatusEffect(StatusEffects.DARKNESS)) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, true, false, false));
        }
    }

    /** Stops the held item from re-bobbing every time the battery ticks down (Fabric item API hook). */
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.REDSTONE);
    }
}
