package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.BisonEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class BisonGroupLeaderGoal extends TargetGoal {
    private final BisonEntity bison;
    private Player nearPlayer;

    public BisonGroupLeaderGoal(BisonEntity bison) {
        super(bison, false);
        this.bison = bison;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        nearPlayer = bison.level().getNearestPlayer(bison, 5.0);
        if (nearPlayer == null || nearPlayer.isCreative() || nearPlayer.isSpectator())
            return false;
        bison.entityData.set(BisonEntity.IS_LEAD, true);
        return true;
    }

    @Override
    public void start() {
        bison.setTarget(nearPlayer);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        return nearPlayer != null && nearPlayer.isAlive()
                && bison.distanceTo(nearPlayer) < 20.0;
    }
}