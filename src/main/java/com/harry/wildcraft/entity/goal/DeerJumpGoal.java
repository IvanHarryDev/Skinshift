package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.DeerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DeerJumpGoal extends Goal {
    private final DeerEntity deer;
    private int jumpCooldown = 0;

    public DeerJumpGoal(DeerEntity deer) {
        this.deer = deer;
        setFlags(EnumSet.of(Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return jumpCooldown <= 0 && deer.isOnGround()
                && deer.getDeltaMovement().horizontalDistanceSqr() > 0.05;
    }

    @Override
    public void start() {
        double hSpeed = deer.getDeltaMovement().horizontalDistanceSqr();
        double jumpPower = hSpeed > 0.1 ? 0.7 : 0.42;
        deer.setDeltaMovement(
                deer.getDeltaMovement().x,
                jumpPower,
                deer.getDeltaMovement().z
        );
        jumpCooldown = 20;
    }

    @Override
    public void tick() {
        if (jumpCooldown > 0) jumpCooldown--;
    }

    @Override
    public boolean canContinueToUse() { return false; }
}