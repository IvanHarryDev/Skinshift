package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.CoyoteEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import java.util.List;

public class CoyotePackAttackGoal extends MeleeAttackGoal {
    private final CoyoteEntity coyote;
    private int packSyncTimer = 0;

    public CoyotePackAttackGoal(CoyoteEntity coyote) {
        super(coyote, 1.3, true);
        this.coyote = coyote;
    }

    @Override
    public boolean canUse() {
        if (coyote.getPackState() != CoyoteEntity.PackState.ATTACKING) return false;
        if (coyote.getHowlAnimTimer() > 0) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (coyote.getPackState() != CoyoteEntity.PackState.ATTACKING) return false;
        if (coyote.getHowlAnimTimer() > 0) return false;
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        packSyncTimer = 0;
    }

    @Override
    public void tick() {
        super.tick();

        packSyncTimer++;
        if (packSyncTimer % 40 == 0 && coyote.getTarget() != null) {
            List<CoyoteEntity> pack = coyote.level().getEntitiesOfClass(
                    CoyoteEntity.class,
                    coyote.getBoundingBox().inflate(CoyoteEntity.PACK_COORDINATION_RANGE),
                    c -> c != coyote && c.isAlive());

            for (CoyoteEntity mate : pack) {
                if (mate.getPackState() != CoyoteEntity.PackState.ATTACKING
                        && mate.getHowlAnimTimer() == 0) {
                    mate.setPackStateDirect(CoyoteEntity.PackState.ATTACKING);
                }
                if (mate.getTarget() == null || !mate.getTarget().isAlive()) {
                    mate.setTarget(coyote.getTarget());
                    mate.setCombatTimer(CoyoteEntity.COMBAT_TIMEOUT);
                }
            }
        }
    }
}