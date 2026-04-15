package com.harry.wildcraft.entity.skinwalker;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SkinwalkerDecoyEventHandler {

    @SubscribeEvent
    public static void onDecoyHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!mob.getPersistentData().getBoolean("SWDecoy")) return;

        event.setCanceled(true);
        SkinwalkerDecoyHelper.redirectDamage(mob, event.getSource(), event.getAmount());
    }
}