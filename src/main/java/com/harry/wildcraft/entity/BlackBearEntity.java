package com.harry.wildcraft.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class BlackBearEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean isSleeping = false;
    private int sitTimer = 0;
    private boolean isSitting = false;
    private static final int SIT_INTERVAL = 400;
    private static final int SIT_DURATION = 100;
    private int deathTimer = 0;
    private static final int DEATH_DELAY_TICKS = 20;

    public BlackBearEntity(EntityType<? extends BlackBearEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE,  30.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 20.0f, 0.7, 0.7,
                e -> !((Player)e).isCreative() && !isAggressive()));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                e -> !((Player)e).isCreative() && e.distanceTo(this) <= 10.0));
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        if (!isAlive()) {
            deathTimer++;
            if (deathTimer < DEATH_DELAY_TICKS) setPersistenceRequired();
            return;
        }

        boolean inCombat = isAggressive() || getTarget() != null;

        if (!level().isDay() && !inCombat) {
            isSleeping = true;
            isSitting  = false;
            getNavigation().stop();
            return;
        } else {
            isSleeping = false;
        }

        if (!inCombat) {
            sitTimer++;
            if (!isSitting && sitTimer >= SIT_INTERVAL) {
                isSitting = true;
                sitTimer  = 0;
                getNavigation().stop();
            }
            if (isSitting) {
                sitTimer++;
                if (sitTimer >= SIT_DURATION) {
                    isSitting = false;
                    sitTimer  = 0;
                }
            }
        } else {
            isSitting = false;
            sitTimer  = 0;
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        triggerAnim("events", "death");
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) {
            isSleeping = false;
            isSitting  = false;
            triggerAnim("events", "hurt");
        }
        return h;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("events", "attack");
        return hit;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (!isAlive())
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.black_bear.death"));
            if (isSleeping)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.sleep"));
            if (isSitting)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.sit"));
            if (isAggressive() && getDeltaMovement().horizontalDistanceSqr() > 0.003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.sprint"));
            if (getDeltaMovement().horizontalDistanceSqr() > 0.0003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.walk"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.black_bear.idle"));
        }));
        registrar.add(new AnimationController<>(this, "events", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().thenPlay("animation.black_bear.attack"))
                .triggerableAnim("hurt",
                        RawAnimation.begin().thenPlay("animation.black_bear.hurt"))
                .triggerableAnim("death",
                        RawAnimation.begin().thenPlay("animation.black_bear.death")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}