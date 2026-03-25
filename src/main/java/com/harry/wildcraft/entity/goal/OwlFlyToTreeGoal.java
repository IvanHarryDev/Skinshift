package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.OwlEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class OwlFlyToTreeGoal extends Goal {
    private final OwlEntity owl;
    private BlockPos targetPos = null;
    private int cooldown = 0;
    private static final int SEARCH_RADIUS = 20;

    public OwlFlyToTreeGoal(OwlEntity owl) {
        this.owl = owl;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        if (!owl.onGround()) return false;
        targetPos = findNearbyLeaves();
        return targetPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetPos != null && !owl.onGround()
                && owl.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        if (targetPos != null) {
            owl.getNavigation().moveTo(
                    targetPos.getX() + 0.5,
                    targetPos.getY() + 1.0,
                    targetPos.getZ() + 0.5,
                    1.0);
        }
    }

    @Override
    public void stop() {
        cooldown = 200 + owl.getRandom().nextInt(200);
        targetPos = null;
    }

    private BlockPos findNearbyLeaves() {
        BlockPos owlPos = owl.blockPosition();
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = owl.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
            int dy = owl.getRandom().nextInt(8) + 2;
            int dz = owl.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS;
            BlockPos candidate = owlPos.offset(dx, dy, dz);
            BlockState state = owl.level().getBlockState(candidate);
            if (state.is(BlockTags.LEAVES)) {
                return candidate;
            }
        }
        return null;
    }
}