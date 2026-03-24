package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.BisonEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class BisonChargeGoal extends Goal {
    private final BisonEntity bison;
    private Player target;
    private int pathUpdateTimer = 0;
    private static final int PATH_UPDATE_INTERVAL = 10;

    public BisonChargeGoal(BisonEntity bison) {
        this.bison = bison;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!bison.isLead()) return false;
        target = bison.level().getNearestPlayer(bison, 50);
        if (target == null || target.isCreative() || target.isSpectator()) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!bison.isLead() || target == null || !target.isAlive()) return false;
        double dist = bison.distanceTo(target);
        return dist <= 50;
    }

    @Override
    public void start() {
        bison.setAggressive(true);
        pathUpdateTimer = 0;
    }

    @Override
    public void stop() {
        bison.setAggressive(false);
        bison.setTarget(null);
        target = null;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;
        bison.setTarget(target);
        bison.getLookControl().setLookAt(target, 30, 30);

        pathUpdateTimer++;
        if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
            pathUpdateTimer = 0;
            bison.getNavigation().moveTo(target, 1.2);
        }

        if (bison.distanceTo(target) < 2.5) {
            bison.setTarget(target);
        }
    }
}