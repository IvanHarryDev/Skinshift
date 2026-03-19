package com.harry.wildcraft.entity.goal;

import com.harry.wildcraft.entity.OwlEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class OwlFlyToTreeGoal extends Goal {
    private final OwlEntity owl;
    private BlockPos targetTree;
    private int searchCooldown = 0;

    public OwlFlyToTreeGoal(OwlEntity owl) {
        this.owl = owl;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (searchCooldown-- > 0) return false;
        if (!owl.onGround() && owl.getDeltaMovement().y == 0) return false;
        targetTree = findNearbyTree();
        return targetTree != null;
    }

    private BlockPos findNearbyTree() {
        Level level = owl.level();
        BlockPos owlPos = owl.blockPosition();
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                for (int dy = 5; dy <= 20; dy++) {
                    BlockPos c = owlPos.offset(dx, dy, dz);
                    if ((level.getBlockState(c).is(BlockTags.LEAVES)
                            || level.getBlockState(c).is(BlockTags.LOGS))
                            && level.getBlockState(c.above()).isAir()) {
                        return c.above();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void start() {
        if (targetTree != null) {
            owl.setNoGravity(true);
            owl.getNavigation().moveTo(
                    targetTree.getX(), targetTree.getY(), targetTree.getZ(), 1.0);
        }
    }

    @Override
    public void tick() {
        if (targetTree == null) { stop(); return; }
        if (owl.distanceToSqr(
                targetTree.getX(), targetTree.getY(), targetTree.getZ()) < 4) {
            owl.setNoGravity(false);
            owl.setDeltaMovement(0, 0, 0);
            stop();
            searchCooldown = 200;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return targetTree != null && !owl.onGround();
    }
}