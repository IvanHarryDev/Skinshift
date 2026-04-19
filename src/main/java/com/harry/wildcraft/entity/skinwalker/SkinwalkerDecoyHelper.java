package com.harry.wildcraft.entity.skinwalker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class SkinwalkerDecoyHelper {

    private static final double MIN_MOVEMENT_THRESHOLD_SQR = 0.002 * 0.002;

    private static final double SWIM_HORIZONTAL_SPEED = 0.08;
    private static final double SWIM_BUOYANCY = 0.04;
    private static final double SWIM_MAX_Y = 0.12;

    @Nullable
    public static Mob createDecoy(SkinwalkerEntity sw, ServerLevel level, EntityType<?> entityType) {
        Entity raw = entityType.create(level);
        if (!(raw instanceof Mob decoy)) {
            if (raw != null) raw.discard();
            return null;
        }

        decoy.getPersistentData().putBoolean("SWDecoy", true);
        decoy.getPersistentData().putUUID("SWOwner", sw.getUUID());

        decoy.moveTo(sw.getX(), sw.getY(), sw.getZ(), sw.getYRot(), sw.getXRot());
        decoy.setPersistenceRequired();

        level.addFreshEntity(decoy);
        return decoy;
    }

    public static void stabilizeDecoy(Mob decoy) {
        if (decoy == null || !decoy.isAlive()) return;

        if (decoy.isInFluidType()) return;

        Vec3 dm = decoy.getDeltaMovement();
        boolean hasPath = !decoy.getNavigation().isDone();
        double horizSqr = dm.x * dm.x + dm.z * dm.z;

        if (!hasPath || horizSqr < MIN_MOVEMENT_THRESHOLD_SQR) {
            decoy.setDeltaMovement(0, dm.y, 0);
        }
    }

    public static void tickDecoySwimming(Mob decoy, Player target) {
        if (decoy == null || !decoy.isAlive() || target == null) return;
        if (!decoy.isInFluidType()) return;

        Vec3 decoyPos = decoy.position();
        Vec3 targetPos = target.position();
        double dx = targetPos.x - decoyPos.x;
        double dz = targetPos.z - decoyPos.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        decoy.getNavigation().stop();

        Vec3 dm = decoy.getDeltaMovement();
        double vx = dm.x;
        double vz = dm.z;

        if (horizDist > 1.2) {
            double nx = dx / horizDist;
            double nz = dz / horizDist;
            vx = nx * SWIM_HORIZONTAL_SPEED;
            vz = nz * SWIM_HORIZONTAL_SPEED;

            float yaw = (float) (Math.atan2(nz, nx) * (180.0 / Math.PI)) - 90f;
            decoy.setYRot(yaw);
            decoy.yBodyRot = yaw;
        } else {
            vx *= 0.5;
            vz *= 0.5;
        }

        double vy = Math.min(dm.y + SWIM_BUOYANCY, SWIM_MAX_Y);

        decoy.setDeltaMovement(vx, vy, vz);
        decoy.resetFallDistance();
        decoy.getLookControl().setLookAt(target, 30f, 30f);
    }

    public static void tickDecoy(SkinwalkerEntity sw, Mob decoy) {
        if (decoy == null || !decoy.isAlive()) return;

        sw.setPos(decoy.getX(), decoy.getY(), decoy.getZ());
        sw.setYRot(decoy.getYRot());
        sw.setXRot(decoy.getXRot());
        sw.setYHeadRot(decoy.getYHeadRot());
        sw.yBodyRot = decoy.yBodyRot;
        sw.setDeltaMovement(decoy.getDeltaMovement());
    }

    public static void freezeDecoy(Mob decoy, Player lookAt) {
        if (decoy == null || !decoy.isAlive()) return;
        decoy.getNavigation().stop();
        Vec3 dm = decoy.getDeltaMovement();
        if (decoy.isInFluidType()) {
            decoy.setDeltaMovement(0, Math.min(dm.y + SkinwalkerDecoyHelper.SWIM_BUOYANCY, SkinwalkerDecoyHelper.SWIM_MAX_Y), 0);
        } else {
            decoy.setDeltaMovement(0, dm.y, 0);
        }
        if (lookAt != null) {
            decoy.getLookControl().setLookAt(lookAt, 30f, 30f);
        }
    }

    public static void stalkTowards(Mob decoy, Player target, double speed) {
        if (decoy == null || !decoy.isAlive() || target == null) return;
        if (decoy.isInFluidType()) return;
        decoy.getNavigation().moveTo(target, speed);
    }

    public static void removeDecoy(@Nullable Mob decoy) {
        if (decoy != null) {
            decoy.discard();
        }
    }

    public static boolean isDecoy(Entity entity) {
        if (entity == null) return false;
        if (!(entity instanceof Mob mob)) return false;
        return mob.getPersistentData().getBoolean("SWDecoy");
    }

    public static boolean redirectDamage(Mob decoy, DamageSource source, float amount) {
        if (!isDecoy(decoy)) return false;
        if (!decoy.getPersistentData().hasUUID("SWOwner")) return false;

        UUID ownerUUID = decoy.getPersistentData().getUUID("SWOwner");
        if (!(decoy.level() instanceof ServerLevel level)) return false;

        List<SkinwalkerEntity> sws = level.getEntitiesOfClass(SkinwalkerEntity.class,
                decoy.getBoundingBox().inflate(50));
        for (SkinwalkerEntity sw : sws) {
            if (sw.getUUID().equals(ownerUUID)) {
                sw.hurt(source, amount);
                return true;
            }
        }
        return false;
    }

    public static int killAllDecoys(ServerLevel level) {
        List<Mob> decoys = level.getEntitiesOfClass(Mob.class,
                level.getWorldBorder().getCollisionShape().bounds().inflate(100),
                SkinwalkerDecoyHelper::isDecoy);
        if (decoys.isEmpty()) {
            decoys = level.getEntitiesOfClass(Mob.class,
                    new net.minecraft.world.phys.AABB(
                            -30000, level.getMinBuildHeight(), -30000,
                            30000, level.getMaxBuildHeight(), 30000),
                    SkinwalkerDecoyHelper::isDecoy);
        }

        int count = decoys.size();
        for (Mob decoy : decoys) {
            decoy.discard();
        }
        return count;
    }
}