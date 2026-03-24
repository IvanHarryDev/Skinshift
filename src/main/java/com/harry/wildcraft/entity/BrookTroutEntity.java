package com.harry.wildcraft.entity;

import com.harry.wildcraft.init.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BrookTroutEntity extends AbstractFish implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int deathTimer = 0;
    private static final int DEATH_DELAY_TICKS = 10;

    public BrookTroutEntity(EntityType<? extends BrookTroutEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes()
                .add(Attributes.MAX_HEALTH, 3.0);
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.COD_BUCKET);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int loot, boolean recent) {
        spawnAtLocation(new ItemStack(ModItems.BROOK_TROUT.get(), 1));
    }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive() && !level().isClientSide) {
            deathTimer++;
            if (deathTimer < DEATH_DELAY_TICKS) setPersistenceRequired();
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        triggerAnim("events", "death");
    }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (!isAlive())
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.brook_trout.death"));
            if (!isInWater())
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.brook_trout.flapping"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.brook_trout.swim"));
        }));
        registrar.add(new AnimationController<>(this, "events", 0,
                state -> PlayState.STOP)
                .triggerableAnim("death",
                        RawAnimation.begin().thenPlay("animation.brook_trout.death")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
