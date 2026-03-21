package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import com.harry.wildcraft.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class SkinwalkerAggressiveGoal extends Goal {
    private final SkinwalkerEntity sw;
    private int pathCheckCooldown = 0;
    private static final int PATH_CHECK_INTERVAL = 40;

    public SkinwalkerAggressiveGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return sw.getMode() == SkinwalkerMode.AGGRESSIVE;
    }

    @Override
    public void start() {
        sw.setMorphed(false);
        sw.setMorphedInto("none");
        sw.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
    }

    @Override
    public void stop() {
        sw.setNoGravity(false);
    }

    @Override
    public void tick() {
        if (!(sw.level() instanceof ServerLevel sl)) return;
        Player target = sl.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null) return;

        sw.setTarget(target);

        pathCheckCooldown--;
        if (pathCheckCooldown <= 0) {
            pathCheckCooldown = PATH_CHECK_INTERVAL;
            checkAndHandlePathBlocked(target);
        }

        String morphed = sw.getMorphedInto();
        boolean isOwl = morphed.equals(ModEntities.OWL.get().getDescriptionId());

        if (isOwl) {
            double dx = target.getX() - sw.getX();
            double dy = target.getY() - sw.getY();
            double dz = target.getZ() - sw.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist > 2.0) {
                double speed = 0.4;
                sw.setDeltaMovement(dx/dist*speed, dy/dist*speed*0.5, dz/dist*speed);
            }
        } else {
            sw.getNavigation().moveTo(target, 1.0);
        }

        if (sw.isPendingThreateningHit() && sw.distanceTo(target) < 2.0) {
            target.hurt(sl.damageSources().mobAttack(sw), 2.0f);
            sw.setPendingThreateningHit(false);
        }
    }

    private void checkAndHandlePathBlocked(Player target) {
        String currentMorph = sw.getMorphedInto();
        boolean isOwl = currentMorph.equals(ModEntities.OWL.get().getDescriptionId());

        Path path = sw.getNavigation().createPath(target, 0);
        boolean canReach = path != null && path.canReach();

        if (!canReach && !isOwl) {
            sw.setMorphedInto(ModEntities.OWL.get().getDescriptionId());
            sw.setMorphed(true);
            sw.setNoGravity(true);
            sw.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        } else if (canReach && isOwl) {
            sw.setMorphed(false);
            sw.setMorphedInto("none");
            sw.setNoGravity(false);
            sw.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
        }
    }
}