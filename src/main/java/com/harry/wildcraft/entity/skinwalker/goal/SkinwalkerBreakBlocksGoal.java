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
    private static final float MAX_HARDNESS = 10.0f;

    public SkinwalkerBreakBlocksGoal(SkinwalkerEntity sw) {
        this.sw = sw;
        setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        return sw.getMode() == SkinwalkerMode.AGGRESSIVE;
    }

    @Override
    public void tick() {
        if (!(sw.level() instanceof ServerLevel level)) return;
        Player target = level.getPlayerByUUID(sw.getTargetPlayerUUID());
        if (target == null) return;

        double lookX = sw.getLookAngle().x * 1.5;
        double lookZ = sw.getLookAngle().z * 1.5;

        BlockPos lowerPos = BlockPos.containing(
                sw.getX() + lookX,
                sw.getY() + 0.5,
                sw.getZ() + lookZ
        );
        tryBreak(level, lowerPos);

        BlockPos upperPos = BlockPos.containing(
                sw.getX() + lookX,
                sw.getY() + 1.5,
                sw.getZ() + lookZ
        );
        tryBreak(level, upperPos);
    }

    private void tryBreak(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0
                && state.getDestroySpeed(level, pos) < MAX_HARDNESS) {
            level.destroyBlock(pos, true);
        }
    }
}