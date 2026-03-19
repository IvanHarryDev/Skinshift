package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.BisonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class BisonChargeGoal extends Goal {
    private final BisonEntity bison;
    private LivingEntity chargeTarget;

    public BisonChargeGoal(BisonEntity bison) {
        this.bison = bison;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!bison.isLead()) return false;
        LivingEntity target = bison.getTarget();
        if (target == null || !target.isAlive()) return false;
        chargeTarget = target;
        return true;
    }

    @Override
    public void tick() {
        if (chargeTarget == null || !chargeTarget.isAlive()) { stop(); return; }
        bison.getNavigation().moveTo(chargeTarget, 1.2);
        bison.setYRot(bison.yBodyRot);
        if (bison.distanceTo(chargeTarget) > 20 && chargeTarget instanceof Player
                && !bison.level().isClientSide) {
            bison.setTarget(null);
            stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return chargeTarget != null && chargeTarget.isAlive() && bison.isLead();
    }
}
