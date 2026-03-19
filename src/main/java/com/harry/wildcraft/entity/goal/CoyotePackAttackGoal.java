package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.CoyoteEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

public class CoyotePackAttackGoal extends Goal {
    private final CoyoteEntity coyote;

    public CoyotePackAttackGoal(CoyoteEntity coyote) {
        this.coyote = coyote;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return coyote.getPackState() == CoyoteEntity.PackState.ATTACKING
                && coyote.getTarget() instanceof Player;
    }

    @Override
    public void tick() {
        Player target = (Player) coyote.getTarget();
        if (target == null || !target.isAlive()) { stop(); return; }

        List<CoyoteEntity> pack = coyote.level().getEntitiesOfClass(
                CoyoteEntity.class,
                coyote.getBoundingBox().inflate(50),
                c -> c != coyote && c.isAlive()
        );
        for (CoyoteEntity packMember : pack) {
            if (packMember.getPackState() != CoyoteEntity.PackState.ATTACKING) {
                packMember.setPackState(CoyoteEntity.PackState.ATTACKING);
                packMember.setTarget(target);
            }
        }

        coyote.getNavigation().moveTo(target, 1.2);
    }

    @Override
    public boolean canContinueToUse() {
        return coyote.getTarget() instanceof Player p && p.isAlive()
                && coyote.distanceTo(p) <= 150;
    }
}