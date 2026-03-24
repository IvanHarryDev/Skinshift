package com.harry.wildcraft.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DeerFemaleEntity extends DeerMaleEntity {

    private final AnimatableInstanceCache femaleCache =
            GeckoLibUtil.createInstanceCache(this);

    public DeerFemaleEntity(EntityType<? extends DeerMaleEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return femaleCache;
    }
}