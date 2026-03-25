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
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BrookTroutEntity extends AbstractFish implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int hurtTimer = 0;
    private static final int HURT_GRACE = 20;

    private int deathTimer = 0;
    private static final int DEATH_DELAY = 40;

    public BrookTroutEntity(EntityType<? extends BrookTroutEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createAttributes()
                .add(Attributes.MAX_HEALTH, 3.0);
    }

    @Override
    protected SoundEvent getFlopSound() { return SoundEvents.COD_FLOP; }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(net.minecraft.world.item.Items.COD_BUCKET);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int loot, boolean recent) {
        spawnAtLocation(new ItemStack(ModItems.BROOK_TROUT.get(), 1));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!isAlive()) {
            deathTimer++;
            if (deathTimer < DEATH_DELAY) setPersistenceRequired();
            return;
        }
        if (hurtTimer > 0) hurtTimer--;
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) hurtTimer = HURT_GRACE;
        return h;
    }

    @Override
    public void die(DamageSource src) { super.die(src); }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 2, state -> {
            if (!isAlive())
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.brook_trout.death"));
            if (!isInWater() && hurtTimer == 0)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.brook_trout.flapping"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.brook_trout.swim"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}