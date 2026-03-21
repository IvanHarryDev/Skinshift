package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class SkinwalkerThreatAttackGoal extends Goal {
    private final SkinwalkerEntity sw;
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TICKS = 60;
    private static final double ATTACK_RANGE = 1.8;

    public SkinwalkerThreatAttackGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        return sw.getMode() == SkinwalkerMode.THREATENING
                && sw.getTargetPlayerUUID() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        if (!(sw.level() instanceof ServerLevel sl)) return;
        Player target = sl.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null) return;

        double dist = sw.distanceTo(target);
        if (dist <= ATTACK_RANGE) {
            target.hurt(sl.damageSources().mobAttack(sw), 2.0f);
            attackCooldown = ATTACK_COOLDOWN_TICKS;
        }
    }
}
