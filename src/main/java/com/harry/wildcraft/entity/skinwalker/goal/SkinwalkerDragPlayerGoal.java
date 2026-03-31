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
    private int eatTimer  = 0;
    private boolean isEatingPhase = false;

    private static final int DRAG_TICKS = 100;
    private static final int EAT_TICKS  = 60;

    public SkinwalkerDragPlayerGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (sw.getMode() != SkinwalkerMode.AGGRESSIVE) return false;
        if (sw.isDraggingPlayer() || isEatingPhase) return true;
        if (sw.getTargetPlayerUUID() == null) return false;
        if (!(sw.level() instanceof ServerLevel sl)) return false;
        Player target = sl.getPlayerByUUID(sw.getTargetPlayerUUID());
        return target != null && target.isAlive() && sw.distanceTo(target) < 2.5;
    }

    @Override
    public void start() {
        ServerLevel sl = (ServerLevel) sw.level();
        draggedPlayer = (ServerPlayer) sl.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (draggedPlayer == null) return;

        dragTimer     = 0;
        eatTimer      = 0;
        isEatingPhase = false;

        sw.setStrongAttacking(true);
        sw.setDraggingPlayer(true);
    }

    @Override
    public void tick() {
        if (draggedPlayer == null || !draggedPlayer.isAlive()) {
            stop();
            return;
        }

        if (isEatingPhase) {
            tickEat();
            return;
        }

        if (!sw.isDraggingPlayer()) {
            stop();
            return;
        }

        dragTimer++;

        Vec3 dir = sw.getLookAngle();
        sw.getMoveControl().setWantedPosition(
                sw.getX() + dir.x * 0.4,
                sw.getY(),
                sw.getZ() + dir.z * 0.4,
                0.7
        );

        draggedPlayer.setPos(
                sw.getX() - dir.x * 1.2,
                sw.getY(),
                sw.getZ() - dir.z * 1.2
        );
        draggedPlayer.setDeltaMovement(0, 0, 0);

        if (dragTimer >= DRAG_TICKS) {
            beginEatSequence();
        }
    }

    private void beginEatSequence() {
        sw.setDraggingPlayer(false);
        isEatingPhase = true;
        eatTimer = 0;

        draggedPlayer.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, EAT_TICKS + 20, 255, false, false
        ));

        sw.setEating(true);
    }

    private void tickEat() {
        eatTimer++;

        if (draggedPlayer != null && draggedPlayer.isAlive()) {
            draggedPlayer.setDeltaMovement(0, 0, 0);
        }

        if (eatTimer == EAT_TICKS / 2) {
            if (draggedPlayer != null && draggedPlayer.isAlive()) {
                draggedPlayer.hurt(sw.level().damageSources().mobAttack(sw), 12.0f);
            }
        }

        if (eatTimer >= EAT_TICKS) {
            sw.setEating(false);

            if (draggedPlayer != null) {
                draggedPlayer.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }

            sw.setScreaming(true);
            sw.setTarget(draggedPlayer);

            stop();
        }
    }

    @Override
    public void stop() {
        sw.setDraggingPlayer(false);
        isEatingPhase = false;
        draggedPlayer = null;
        dragTimer     = 0;
        eatTimer      = 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (draggedPlayer == null || !draggedPlayer.isAlive()) return false;
        return sw.isDraggingPlayer() || isEatingPhase;
    }
}