package com.harry.wildcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class SightHelper {

    public static boolean isPlayerLookingAt(Entity player, Vec3 targetPos, double threshold) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 toTarget = targetPos.subtract(eyePos).normalize();
        Vec3 look = player.getLookAngle().normalize();
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
                    player.getX() + dx, 0, player.getZ() + dz
            );

            Vec3 dirToCandidate = candidate.subtract(player.position()).normalize();
            double dot = look.dot(new Vec3(dirToCandidate.x, 0, dirToCandidate.z).normalize());

            if (dot > 0.3) continue;

            BlockPos surfacePos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(candidate.x, 0, candidate.z)
            );

            if (surfacePos.getY() <= level.getMinBuildHeight()) continue;

            if (!isSafeGround(level, surfacePos)) continue;
            if (!isSafeSpawnArea(level, surfacePos)) continue;

            return new Vec3(surfacePos.getX() + 0.5, surfacePos.getY(), surfacePos.getZ() + 0.5);
        }
        return null;
    }

    public static Vec3 findSafePosition(ServerLevel level, double x, double z) {
        BlockPos surfacePos = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(x, 0, z)
        );

        if (surfacePos.getY() <= level.getMinBuildHeight()) return null;
        if (!isSafeGround(level, surfacePos)) return null;
        if (!isSafeSpawnArea(level, surfacePos)) return null;

        return new Vec3(surfacePos.getX() + 0.5, surfacePos.getY(), surfacePos.getZ() + 0.5);
    }

    private static boolean isSafeGround(ServerLevel level, BlockPos pos) {
        BlockState stateAt = level.getBlockState(pos);
        if (!stateAt.getFluidState().isEmpty()) return false;
        if (stateAt.is(Blocks.WATER) || stateAt.is(Blocks.LAVA)) return false;

        BlockState stateAbove = level.getBlockState(pos.above());
        if (!stateAbove.getFluidState().isEmpty()) return false;
        if (stateAbove.is(Blocks.WATER) || stateAbove.is(Blocks.LAVA)) return false;

        BlockState stateAbove2 = level.getBlockState(pos.above(2));
        if (!stateAbove2.getFluidState().isEmpty()) return false;
        if (stateAbove2.is(Blocks.WATER) || stateAbove2.is(Blocks.LAVA)) return false;

        BlockPos below = pos.below();
        BlockState stateBelow = level.getBlockState(below);
        if (!stateBelow.getFluidState().isEmpty()) return false;
        if (stateBelow.is(Blocks.WATER) || stateBelow.is(Blocks.LAVA)) return false;
        if (!stateBelow.isSolid()) return false;

        return true;
    }

    private static boolean isSafeSpawnArea(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos neighbor = below.offset(dx, 0, dz);
                BlockState ns = level.getBlockState(neighbor);
                if (!ns.getFluidState().isEmpty()) return false;
                if (ns.is(Blocks.WATER) || ns.is(Blocks.LAVA)) return false;

                BlockPos neighborAt = pos.offset(dx, 0, dz);
                BlockState nas = level.getBlockState(neighborAt);
                if (!nas.getFluidState().isEmpty()) return false;
            }
        }
        return true;
    }
}