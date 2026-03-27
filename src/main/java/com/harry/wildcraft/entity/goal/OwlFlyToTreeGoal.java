package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.OwlEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;

public class OwlFlyToTreeGoal extends Goal {
    private final OwlEntity owl;
    private Vec3 targetVec = null;
    private BlockPos targetBlock = null;
    private int cooldown = 0;
    private int flightTicks = 0;
    private int stuckTicks = 0;
    private Vec3 lastPos = null;
    private int retargetAttempts = 0;

    private static final int SEARCH_RADIUS     = 20;
    private static final int MAX_FLIGHT_TICKS  = 300;
    private static final int MAX_STUCK_TICKS   = 40;
    private static final int MAX_RETARGETS     = 3;
    private static final double ARRIVAL_DIST   = 1.5;
    private static final double STUCK_THRESHOLD = 0.15;

    public OwlFlyToTreeGoal(OwlEntity owl) {
        this.owl = owl;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        if (owl.isFlying()) return false;
        return tryFindTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (targetVec == null) return false;
        if (flightTicks > MAX_FLIGHT_TICKS) return false;
        if (retargetAttempts > MAX_RETARGETS) return false;

        if (owl.isLanded() && flightTicks > 10) return false;

        return true;
    }

    @Override
    public void start() {
        flightTicks = 0;
        stuckTicks = 0;
        retargetAttempts = 0;
        lastPos = owl.position();

        Vec3 mov = owl.getDeltaMovement();
        owl.setDeltaMovement(mov.x, 0.35, mov.z);
    }

    @Override
    public void tick() {
        flightTicks++;
        if (targetVec == null) return;

        if (flightTicks % 20 == 0) {
            if (lastPos != null) {
                double moved = owl.position().distanceTo(lastPos);
                if (moved < STUCK_THRESHOLD) {
                    stuckTicks += 20;
                } else {
                    stuckTicks = 0;
                }
            }
            lastPos = owl.position();

            if (stuckTicks >= MAX_STUCK_TICKS) {
                retargetAttempts++;
                stuckTicks = 0;
                if (!tryFindTarget()) {
                    targetVec = null;
                    return;
                }
                double angle = owl.getRandom().nextDouble() * Math.PI * 2;
                Vec3 mov = owl.getDeltaMovement();
                owl.setDeltaMovement(
                        mov.x + Math.cos(angle) * 0.2,
                        Math.max(mov.y, 0.15),
                        mov.z + Math.sin(angle) * 0.2
                );
                return;
            }
        }

        if (flightTicks % 10 == 0 && isPathObstructed()) {
            retargetAttempts++;
            stuckTicks = 0;
            if (!tryFindTarget()) {
                targetVec = null;
                return;
            }
        }

        double dx = targetVec.x - owl.getX();
        double dy = targetVec.y - owl.getY();
        double dz = targetVec.z - owl.getZ();
        double distH = Math.sqrt(dx * dx + dz * dz);
        double distTotal = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distTotal < ARRIVAL_DIST) {
            Vec3 mov = owl.getDeltaMovement();
            owl.setDeltaMovement(mov.x * 0.5, -0.05, mov.z * 0.5);
            return;
        }

        double speed;
        if (distH > 8.0) {
            speed = 0.28;
        } else if (distH > 3.0) {
            speed = 0.18;
        } else {
            speed = 0.10;
        }

        double vx = 0, vz = 0;
        if (distH > 0.3) {
            vx = (dx / distH) * speed;
            vz = (dz / distH) * speed;
        }

        // Vertical
        double vy;
        if (dy > 2.0) {
            vy = 0.18;
        } else if (dy > 0.5) {
            vy = dy * 0.06;
        } else if (dy < -2.0) {
            vy = -0.06;
        } else if (dy < -0.3) {
            vy = dy * 0.04;
        } else {
            vy = 0.0;
        }

        Vec3 current = owl.getDeltaMovement();
        double blend = 0.25;
        owl.setDeltaMovement(
                current.x * (1 - blend) + vx * blend,
                current.y * (1 - blend) + vy * blend,
                current.z * (1 - blend) + vz * blend
        );

        if (distH > 0.5) {
            double yaw = Math.toDegrees(Math.atan2(-dx, dz));
            owl.setYRot((float) yaw);
            owl.yBodyRot = (float) yaw;
        }
    }

    @Override
    public void stop() {
        cooldown = 200 + owl.getRandom().nextInt(200);
        targetVec = null;
        targetBlock = null;
        flightTicks = 0;
        stuckTicks = 0;
        retargetAttempts = 0;
        lastPos = null;
    }

    private boolean tryFindTarget() {
        BlockPos owlPos = owl.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = owl.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
            int dy = owl.getRandom().nextInt(10) + 2;
            int dz = owl.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
            BlockPos candidate = owlPos.offset(dx, dy, dz);

            if (!isValidLandingSpot(candidate)) continue;

            double dist = candidate.distSqr(owlPos);
            double heightPenalty = Math.abs(dy) * 2.0;
            double score = dist + heightPenalty;

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best != null) {
            targetBlock = best;
            double landY = best.getY() + getBlockTopHeight(best);
            targetVec = new Vec3(best.getX() + 0.5, landY, best.getZ() + 0.5);
            return true;
        }
        return false;
    }

    private boolean isValidLandingSpot(BlockPos pos) {
        BlockState state = owl.level().getBlockState(pos);

        boolean isSolid = state.isSolid();
        boolean isLeaves = state.is(BlockTags.LEAVES);
        if (!isSolid && !isLeaves) return false;

        BlockPos above1 = pos.above();
        BlockPos above2 = pos.above(2);

        if (!isPassable(above1)) return false;
        if (!isPassable(above2)) return false;

        return true;
    }

    private boolean isPassable(BlockPos pos) {
        BlockState state = owl.level().getBlockState(pos);
        if (state.isAir()) return true;

        if (!state.isSolid()) return true;

        try {
            VoxelShape shape = state.getCollisionShape(owl.level(), pos);
            return shape.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private double getBlockTopHeight(BlockPos pos) {
        BlockState state = owl.level().getBlockState(pos);
        try {
            VoxelShape shape = state.getShape(owl.level(), pos);
            if (shape.isEmpty()) return 1.0;
            double maxY = shape.max(Direction.Axis.Y);
            if (Double.isInfinite(maxY) || maxY <= 0.0) return 1.0;
            return maxY;
        } catch (Exception e) {
            return 1.0;
        }
    }

    private boolean isPathObstructed() {
        if (targetVec == null) return false;

        Vec3 owlPos = owl.position();
        Vec3 dir = targetVec.subtract(owlPos);
        double dist = dir.length();
        if (dist < 2.0) return false;

        Vec3 step = dir.normalize();
        int steps = Math.min((int) dist, 8);

        for (int i = 1; i <= steps; i++) {
            Vec3 checkPoint = owlPos.add(step.scale(i));
            BlockPos checkBlock = BlockPos.containing(checkPoint);
            BlockState state = owl.level().getBlockState(checkBlock);

            if (state.isAir()) continue;

            if (!state.isSolid()) continue;

            try {
                VoxelShape shape = state.getCollisionShape(owl.level(), checkBlock);
                if (!shape.isEmpty()) return true;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }
}