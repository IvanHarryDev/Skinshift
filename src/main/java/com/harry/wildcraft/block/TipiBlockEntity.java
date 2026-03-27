package com.harry.wildcraft.block;

import com.harry.wildcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TipiBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public TipiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIPI_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos pos = getBlockPos();
        return new AABB(
                pos.getX() - 2.0, pos.getY() - 1.0, pos.getZ() - 2.0,
                pos.getX() + 3.0, pos.getY() + 5.0, pos.getZ() + 3.0
        );
    }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 0,
                state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}