package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SkinwalkerDragPlayerGoal extends Goal {
    private final SkinwalkerEntity sw;
    private ServerPlayer draggedPlayer;
    private int dragTimer = 0;
    private static final int DRAG_TICKS = 100;

    public SkinwalkerDragPlayerGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (sw.getMode() != SkinwalkerMode.AGGRESSIVE) return false;
        if (sw.getTargetPlayerUUID() == null) return false;
        if (!(sw.level() instanceof ServerLevel sl)) return false;
        Player target = sl.getPlayerByUUID(sw.getTargetPlayerUUID());
        return target != null && sw.distanceTo(target) < 2.5;
    }

    @Override
    public void start() {
        ServerLevel sl = (ServerLevel) sw.level();
        draggedPlayer = (ServerPlayer) sl.getPlayerByUUID(sw.getTargetPlayerUUID());
        dragTimer = 0;
        sw.setDraggingPlayer(true);
    }

    @Override
    public void tick() {
        if (draggedPlayer == null || !draggedPlayer.isAlive()) { stop(); return; }
        dragTimer++;
        Vec3 dir = sw.getLookAngle();
        sw.getMoveControl().setWantedPosition(
                sw.getX() + dir.x * 0.4, sw.getY(), sw.getZ() + dir.z * 0.4, 0.7);
        draggedPlayer.setPos(
                sw.getX() - dir.x * 1.2, sw.getY(), sw.getZ() - dir.z * 1.2);
        if (dragTimer >= DRAG_TICKS) triggerEatSequence();
    }

    private void triggerEatSequence() {
        draggedPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 255));
        sw.setEating(true);
        draggedPlayer.hurt(sw.level().damageSources().mobAttack(sw), 12.0f);
        sw.setEating(false);
        sw.setScreaming(true);
        sw.setTarget(draggedPlayer);
        stop();
    }

    @Override
    public void stop() {
        sw.setDraggingPlayer(false);
        draggedPlayer = null;
    }

    @Override
    public boolean canContinueToUse() {
        return dragTimer < DRAG_TICKS && draggedPlayer != null && draggedPlayer.isAlive();
    }
}