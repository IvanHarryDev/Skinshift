package com.harry.wildcraft.entity;

import com.harry.wildcraft.init.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BrookTroutEntity extends AbstractFish implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BrookTroutEntity(EntityType<? extends BrookTroutEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getBucketItem() {
        return ModItems.BROOK_TROUT.get();
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 3.0);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int lootLevel, boolean recentHit) {
        spawnAtLocation(new ItemStack(ModItems.BROOK_TROUT.get(), 1));
    }

    // GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "swim", 5, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.brook_trout.swim"))
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
