package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.DeerMaleEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DeerJumpGoal extends Goal {
    private final DeerMaleEntity deer;
    private int jumpCooldown = 0;

    public DeerJumpGoal(DeerMaleEntity deer) {
        this.deer = deer;
        setFlags(EnumSet.of(Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return deer.onGround()
                && deer.getDeltaMovement().horizontalDistanceSqr() > 0.04
                && jumpCooldown <= 0;
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        Vec3 mov = deer.getDeltaMovement();
        deer.setDeltaMovement(mov.x * 1.2, 0.8, mov.z * 1.2);
        jumpCooldown = 20;
        deer.hasImpulse = true;
    }

    @Override
    public void tick() {
        if (jumpCooldown > 0) jumpCooldown--;
    }
}