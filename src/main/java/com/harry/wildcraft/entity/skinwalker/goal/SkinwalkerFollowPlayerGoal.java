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

    public SkinwalkerFollowPlayerGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() { return sw.getMode() != SkinwalkerMode.AGGRESSIVE; }

    @Override
    public void tick() {
        if (!(sw.level() instanceof ServerLevel level)) return;
        if (sw.getTargetPlayerUUID() == null) return;
        target = level.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null) return;

        double dist = sw.distanceTo(target);
        boolean playerLooking = SightHelper.isPlayerLookingAt(target, sw.position(), 0.95);

        if (sw.getMode() == SkinwalkerMode.PASSIVE) {
            if (playerLooking) {
                sw.getNavigation().stop();
                sw.getLookControl().setLookAt(target, 30, 30);
                return;
            }
            if (dist > 50) {
                sw.getNavigation().moveTo(target, 0.7);
            } else if (dist < 10) {
                sw.getNavigation().moveTo(
                        sw.getX() + (sw.getX() - target.getX()),
                        sw.getY(),
                        sw.getZ() + (sw.getZ() - target.getZ()), 0.7);
            }
            SkinwalkerMorphHelper.checkAndMorphIfBiomeChanged(sw, level);
        }

        if (sw.getMode() == SkinwalkerMode.THREATENING) {
            if (playerLooking) {
                sw.getNavigation().stop();
                sw.getLookControl().setLookAt(target, 30, 30);
                sw.incrementLookAtTimer();
                if (sw.getLookAtTimer() > 100 || dist < 5) {
                    sw.poofAndRespawn(level, target);
                }
                return;
            }
            sw.setLookAtTimer(0);
            if (dist > 10) sw.getNavigation().moveTo(target, 0.8);
            if (!level.isDay() && dist > 15 && dist < 35) {
                sw.getNavigation().stop();
                sw.getLookControl().setLookAt(target, 30, 30);
            }
            if (level.isDay() && !sw.isMorphed()) {
                SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, sw.position());
            }
        }
    }
}