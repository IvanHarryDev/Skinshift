package com.harry.wildcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class SightHelper {

    public static boolean isPlayerLookingAt(
            Player player, Entity target, double dotThreshold) {
        Vec3 look     = player.getLookAngle().normalize();
        Vec3 toTarget = target.position()
                .subtract(player.position()).normalize();
        return look.dot(toTarget) > dotThreshold;
    }

    public static boolean isPlayerLookingAt(
            Player player, Vec3 targetPos, double dotThreshold) {
        Vec3 look     = player.getLookAngle().normalize();
        Vec3 toTarget = targetPos.subtract(player.position()).normalize();
        return look.dot(toTarget) > dotThreshold;
    }

    public static Vec3 findPositionOutOfSight(
            ServerLevel level, ServerPlayer player,
            int distance, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            int y = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos((int) x, 0, (int) z)).getY();
            Vec3 candidate = new Vec3(x, y, z);
            if (!isPlayerLookingAt(player, candidate, 0.8)) {
                return candidate;
            }
        }
        return null;
    }
}