package com.harry.wildcraft.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class EntityHelper {

    public static boolean isInSurvivalOrAdventure(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    public static <T extends Entity> List<T> getNearby(
            ServerLevel level, Entity center, Class<T> clazz, double radius) {
        AABB box = center.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(clazz, box, e -> e != center && e.isAlive());
    }

    public static void lookAt(Mob mob, Entity target) {
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
    }

    public static boolean hasLineOfSight(LivingEntity from, Entity to) {
        return from.hasLineOfSight(to);
    }
}