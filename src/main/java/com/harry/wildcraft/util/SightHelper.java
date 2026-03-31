package com.harry.wildcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class SightHelper {

    public static boolean isPlayerLookingAt(Entity player, Vec3 targetPos, double threshold) {
        Vec3 eyePos   = player.getEyePosition();
        Vec3 toTarget = targetPos.subtract(eyePos).normalize();
        Vec3 look     = player.getLookAngle().normalize();
        return look.dot(toTarget) >= threshold;
    }

    public static Vec3 findPositionOutOfSight(ServerLevel level, ServerPlayer player,
                                              int distance, int maxAttempts) {
        Vec3 look = player.getLookAngle().normalize();

        for (int i = 0; i < maxAttempts; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dx = Math.cos(angle) * distance;
            double dz = Math.sin(angle) * distance;

            Vec3 candidate = new Vec3(
                    player.getX() + dx,
                    0,
                    player.getZ() + dz
            );

            Vec3 dirToCandidate = candidate.subtract(player.position()).normalize();
            double dot = look.dot(new Vec3(dirToCandidate.x, 0, dirToCandidate.z).normalize());

            if (dot > 0.3) continue;

            BlockPos surfacePos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(candidate.x, 0, candidate.z)
            );

            if (surfacePos.getY() <= level.getMinBuildHeight()) continue;

            return new Vec3(surfacePos.getX() + 0.5, surfacePos.getY(), surfacePos.getZ() + 0.5);
        }
        return null;
    }
}