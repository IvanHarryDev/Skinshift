package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class SkinwalkerAggressiveGoal extends Goal {
    private final SkinwalkerEntity sw;

    public SkinwalkerAggressiveGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() { return sw.getMode() == SkinwalkerMode.AGGRESSIVE; }

    @Override
    public void start() {
        sw.setMorphed(false);
        sw.setMorphedInto("none");
        sw.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
    }

    @Override
    public void tick() {
        if (!(sw.level() instanceof ServerLevel level)) return;
        if (sw.getTargetPlayerUUID() == null) return;
        Player target = level.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null) return;
        sw.setTarget(target);
        sw.getNavigation().moveTo(target, 1.0);
        if (sw.isPendingThreateningHit() && sw.distanceTo(target) < 2.0) {
            target.hurt(level.damageSources().mobAttack(sw), 2.0f);
            sw.setPendingThreateningHit(false);
        }
    }
}