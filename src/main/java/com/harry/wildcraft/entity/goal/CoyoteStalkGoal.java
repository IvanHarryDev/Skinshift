package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.CoyoteEntity;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CoyoteStalkGoal extends Goal {
    private final CoyoteEntity coyote;
    private Player target;
    private int fleeCooldown  = 0;
    private int stalkTimer    = 0;
    private static final int FLEE_DURATION   = 40;
    private static final double STALK_RANGE  = 150.0;
    private static final double ATTACK_RANGE = 30.0;
    private static final double RUSH_RANGE   = 5.0;

    public CoyoteStalkGoal(CoyoteEntity coyote) {
        this.coyote = coyote;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (coyote.getPackState() == CoyoteEntity.PackState.ATTACKING) return false;
        if (!(coyote.level() instanceof ServerLevel sl)) return false;
        target = sl.getNearestPlayer(coyote, STALK_RANGE);
        if (target == null || target.isCreative() || target.isSpectator()) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (coyote.getPackState() == CoyoteEntity.PackState.ATTACKING) return false;
        return target != null && target.isAlive()
                && !target.isCreative() && !target.isSpectator()
                && coyote.distanceTo(target) <= STALK_RANGE;
    }

    @Override
    public void start() {
        coyote.setPackState(CoyoteEntity.PackState.STALKING);
        fleeCooldown = 0;
        stalkTimer   = 0;
    }

    @Override
    public void stop() {
        if (coyote.getPackState() == CoyoteEntity.PackState.STALKING)
            coyote.setPackState(CoyoteEntity.PackState.IDLE);
        coyote.getNavigation().stop();
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = coyote.distanceTo(target);
        boolean playerLooking = SightHelper.isPlayerLookingAt(target, coyote.position(), 0.92);

        if (dist <= RUSH_RANGE) {
            coyote.setPackState(CoyoteEntity.PackState.ATTACKING);
            coyote.setTarget(target);
            return;
        }

        if (playerLooking && dist > ATTACK_RANGE && fleeCooldown == 0) {
            fleeCooldown = FLEE_DURATION;
        }
        if (fleeCooldown > 0) {
            fleeCooldown--;
            Vec3 away = coyote.position().subtract(target.position()).normalize();
            coyote.getNavigation().moveTo(
                    coyote.getX() + away.x * 8,
                    coyote.getY(),
                    coyote.getZ() + away.z * 8,
                    0.9);
            return;
        }

        stalkTimer++;
        if (stalkTimer % 40 == 0) {
            double angle = Math.PI * 2 * coyote.getRandom().nextDouble();
            double radius = Math.max(dist - 3.0, ATTACK_RANGE * 0.8);
            double tx = target.getX() + Math.cos(angle) * radius;
            double tz = target.getZ() + Math.sin(angle) * radius;
            coyote.getNavigation().moveTo(tx, target.getY(), tz, 0.7);
        }
        coyote.getLookControl().setLookAt(target, 30, 30);
    }
}