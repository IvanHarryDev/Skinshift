package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMorphHelper;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class SkinwalkerFollowPlayerGoal extends Goal {

    private final SkinwalkerEntity sw;
    private Player target;
    private boolean wasObservingAtNight = false;
    private int wanderCooldown = 0;

    public SkinwalkerFollowPlayerGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return sw.getMode() != SkinwalkerMode.AGGRESSIVE
                && sw.getTargetPlayerUUID() != null;
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        if (!(sw.level() instanceof ServerLevel level)) return;
        target = level.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null) return;

        double dist = sw.distanceTo(target);
        boolean playerLooking = SightHelper.isPlayerLookingAt(target, sw.position(), 0.95);

        if (sw.getMode() == SkinwalkerMode.PASSIVE) {
            tickPassive(level, dist, playerLooking);
        } else if (sw.getMode() == SkinwalkerMode.THREATENING) {
            tickThreatening(level, dist, playerLooking);
        }
    }

    //  PASSIVE MODE
    private void tickPassive(ServerLevel level, double dist, boolean playerLooking) {

        if (playerLooking) {
            sw.getNavigation().stop();
            sw.getLookControl().setLookAt(target, 30, 30);
            return;
        }

        if (dist > 50) {
            sw.getNavigation().moveTo(target, 0.7);
        } else if (dist < 10) {
            double awayX = sw.getX() + (sw.getX() - target.getX()) * 0.5;
            double awayZ = sw.getZ() + (sw.getZ() - target.getZ()) * 0.5;
            sw.getNavigation().moveTo(awayX, sw.getY(), awayZ, 0.7);
        } else {
            wanderCooldown--;
            if (wanderCooldown <= 0) {
                wanderCooldown = 80 + sw.getRandom().nextInt(120);
                double offsetX = (sw.getRandom().nextDouble() - 0.5) * 20;
                double offsetZ = (sw.getRandom().nextDouble() - 0.5) * 20;
                double wanderX = sw.getX() + offsetX;
                double wanderZ = sw.getZ() + offsetZ;
                double dxFromPlayer = wanderX - target.getX();
                double dzFromPlayer = wanderZ - target.getZ();
                double wanderDist = Math.sqrt(dxFromPlayer * dxFromPlayer + dzFromPlayer * dzFromPlayer);
                if (wanderDist > 50) {
                    wanderX = target.getX() + (dxFromPlayer / wanderDist) * 48;
                    wanderZ = target.getZ() + (dzFromPlayer / wanderDist) * 48;
                }
                sw.getNavigation().moveTo(wanderX, sw.getY(), wanderZ, 0.5);
            }
        }
        SkinwalkerMorphHelper.checkAndMorphIfBiomeChanged(sw, level);
    }

    //  THREATENING MODE
    private void tickThreatening(ServerLevel level, double dist, boolean playerLooking) {

        if (playerLooking) {
            sw.getNavigation().stop();
            sw.getLookControl().setLookAt(target, 30, 30);
            sw.incrementLookAtTimer();

            if (sw.getLookAtTimer() > 100 || dist < 5) {
                sw.poofAndRespawn(level, target);
                wasObservingAtNight = false;
            }
            return;
        }
        sw.setLookAtTimer(0);

        boolean isNight = !level.isDay();
        boolean inObserveRange = dist > 15 && dist < 35;

        if (isNight && inObserveRange) {
            sw.getNavigation().stop();
            sw.getLookControl().setLookAt(target, 30, 30);

            if (!wasObservingAtNight) {
                sw.setMorphed(false);
                sw.setMorphedInto("none");
                sw.randomizeNocturnalCrouch();
                wasObservingAtNight = true;
            }
            return;
        }

        if (wasObservingAtNight) {
            wasObservingAtNight = false;
            if (!sw.isMorphed()) {
                SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
            }
        }

        if (level.isDay() && !sw.isMorphed()) {
            SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
        }

        if (dist > 10) {
            sw.getNavigation().moveTo(target, 0.8);
        } else if (dist < 8) {
            double awayX = sw.getX() + (sw.getX() - target.getX()) * 0.3;
            double awayZ = sw.getZ() + (sw.getZ() - target.getZ()) * 0.3;
            sw.getNavigation().moveTo(awayX, sw.getY(), awayZ, 0.6);
        }

        SkinwalkerMorphHelper.checkAndMorphIfBiomeChanged(sw, level);
    }
}