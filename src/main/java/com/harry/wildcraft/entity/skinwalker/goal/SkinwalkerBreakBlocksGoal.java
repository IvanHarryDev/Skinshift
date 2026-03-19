package com.harry.wildcraft.entity.skinwalker.goal;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class SkinwalkerBreakBlocksGoal extends Goal {
    private final SkinwalkerEntity sw;

    public SkinwalkerBreakBlocksGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() { return sw.getMode() == SkinwalkerMode.AGGRESSIVE; }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) sw.level();
        Player target = level.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null) return;

        BlockPos frontPos = BlockPos.containing(
                sw.getX() + sw.getLookAngle().x * 1.5,
                sw.getY() + 0.5,
                sw.getZ() + sw.getLookAngle().z * 1.5);

        BlockState state = level.getBlockState(frontPos);
        if (!state.isAir() && state.getDestroySpeed(level, frontPos) < 10.0f) {
            level.destroyBlock(frontPos, true);
        }
    }
}