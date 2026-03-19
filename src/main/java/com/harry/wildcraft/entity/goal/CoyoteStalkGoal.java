package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.CoyoteEntity;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CoyoteStalkGoal extends Goal {
    private final CoyoteEntity coyote;
    private Player stalkedPlayer;

    public CoyoteStalkGoal(CoyoteEntity coyote) {
        this.coyote = coyote;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (coyote.getPackState() == CoyoteEntity.PackState.ATTACKING) return false;
        stalkedPlayer = coyote.level().getNearestPlayer(coyote, 150.0);
        if (stalkedPlayer == null || stalkedPlayer.isCreative()) return false;
        coyote.setPackState(CoyoteEntity.PackState.STALKING);
        return true;
    }

    @Override
    public void tick() {
        if (stalkedPlayer == null) return;
        double dist = coyote.distanceTo(stalkedPlayer);

        if (dist > 30 && SightHelper.isPlayerLookingAt(stalkedPlayer, coyote.position(), 0.9)) {
            Vec3 away = coyote.position().subtract(stalkedPlayer.position()).normalize().scale(10);
            coyote.getNavigation().moveTo(
                    coyote.getX() + away.x, coyote.getY(), coyote.getZ() + away.z, 1.0);
            return;
        }

        if (dist > 5) {
            coyote.getNavigation().moveTo(stalkedPlayer, 0.6);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return stalkedPlayer != null && stalkedPlayer.isAlive()
                && coyote.getPackState() != CoyoteEntity.PackState.ATTACKING;
    }
}
