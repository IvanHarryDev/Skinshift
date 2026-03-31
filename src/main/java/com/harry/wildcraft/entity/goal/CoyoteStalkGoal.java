package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.CoyoteEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class CoyoteStalkGoal extends Goal {
    private final CoyoteEntity coyote;
    private Player stalkTarget = null;
    private int repositionTimer = 0;

    public CoyoteStalkGoal(CoyoteEntity coyote) {
        this.coyote = coyote;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (coyote.getPackState() == CoyoteEntity.PackState.ATTACKING) return false;
        if (coyote.getPackState() == CoyoteEntity.PackState.HOWLING) return false;
        if (coyote.getHowlAnimTimer() > 0) return false;
        if (coyote.level().isClientSide) return false;

        List<CoyoteEntity> stalkingPack = coyote.level().getEntitiesOfClass(
                CoyoteEntity.class,
                coyote.getBoundingBox().inflate(CoyoteEntity.PACK_COORDINATION_RANGE),
                c -> c != coyote && c.isAlive()
                        && c.getPackState() == CoyoteEntity.PackState.STALKING);

        if (!stalkingPack.isEmpty()) {
            Player target = coyote.level().getNearestPlayer(
                    coyote, CoyoteEntity.MAX_CHASE_RANGE);
            if (target != null && !target.isCreative() && !target.isSpectator()) {
                stalkTarget = target;
                return true;
            }
        }

        stalkTarget = coyote.level().getNearestPlayer(
                coyote, CoyoteEntity.STALK_DETECT_RANGE);
        if (stalkTarget == null) return false;
        return !stalkTarget.isCreative() && !stalkTarget.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        if (coyote.getPackState() == CoyoteEntity.PackState.ATTACKING) return false;
        if (coyote.getPackState() == CoyoteEntity.PackState.HOWLING) return false;
        if (coyote.getHowlAnimTimer() > 0) return false;
        return stalkTarget != null && stalkTarget.isAlive()
                && !stalkTarget.isCreative() && !stalkTarget.isSpectator()
                && coyote.distanceTo(stalkTarget) <= CoyoteEntity.MAX_CHASE_RANGE;
    }

    @Override
    public void start() {
        coyote.setPackStateDirect(CoyoteEntity.PackState.STALKING);
        coyote.setCombatTimer(0);
        repositionTimer = 0;
        coyote.fleeFromGazeTimer = 0;

        coyote.alertPackToStalk(stalkTarget);
    }

    @Override
    public void stop() {
        if (coyote.getPackState() == CoyoteEntity.PackState.STALKING) {
            coyote.setPackStateDirect(CoyoteEntity.PackState.IDLE);
        }
        stalkTarget = null;
        coyote.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (stalkTarget == null) return;

        double dist = coyote.distanceTo(stalkTarget);

        if (dist < CoyoteEntity.ATTACK_TRIGGER_RANGE) {
            if (!coyote.hasHowledForCombat()) {
                coyote.setHasHowledForCombat(true);
                coyote.fireHowlThenAttack(stalkTarget);
            } else {
                coyote.setPackStateAll(CoyoteEntity.PackState.ATTACKING);
                coyote.setTarget(stalkTarget);
            }
            return;
        }

        coyote.getLookControl().setLookAt(stalkTarget, 30, 30);

        if (dist > 8.0 && coyote.fleeFromGazeTimer == 0
                && coyote.isPlayerLookingAtMe(stalkTarget)) {
            coyote.fleeFromGazeTimer = CoyoteEntity.FLEE_GAZE_DURATION;
        }

        if (coyote.fleeFromGazeTimer > 0) {
            coyote.fleeFromGazeTimer--;
            Vec3 away = coyote.position().subtract(stalkTarget.position()).normalize();
            coyote.getNavigation().moveTo(
                    coyote.getX() + away.x * 8,
                    coyote.getY(),
                    coyote.getZ() + away.z * 8, 1.2);
            return;
        }

        repositionTimer++;
        if (repositionTimer % 20 == 0) {
            double angleOffset = (coyote.getId() % 6) * (Math.PI * 2.0 / 6.0);
            double angle  = angleOffset + (repositionTimer * 0.015);
            double radius = Math.max(dist * 0.6, 8.0);
            radius = Math.max(radius - (repositionTimer * 0.01),
                    CoyoteEntity.ATTACK_TRIGGER_RANGE + 2);

            coyote.getNavigation().moveTo(
                    stalkTarget.getX() + Math.cos(angle) * radius,
                    stalkTarget.getY(),
                    stalkTarget.getZ() + Math.sin(angle) * radius,
                    0.65);
        }
    }
}