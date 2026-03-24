package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.BisonChargeGoal;
import com.harry.wildcraft.entity.goal.BisonGroupLeaderGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

public class BisonEntity extends Animal implements GeoEntity {

    public static final EntityDataAccessor<Boolean> IS_LEAD =
            SynchedEntityData.defineId(BisonEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int deathTimer = 0;
    private static final int DEATH_DELAY_TICKS = 30;

    public BisonEntity(EntityType<? extends BisonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_LEAD, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.FOLLOW_RANGE,  50.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(1, new BisonChargeGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 20.0f, 0.5, 0.5,
                e -> !((Player)e).isCreative() && !isAggressive()));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(1, new BisonGroupLeaderGoal(this));
    }

    public boolean isLead() { return entityData.get(IS_LEAD); }
    public void setLead(boolean v) { entityData.set(IS_LEAD, v); }

    @Override
    public void tick() {
        super.tick();
        if (!isAlive() && !level().isClientSide) {
            deathTimer++;
            if (deathTimer < DEATH_DELAY_TICKS) {
                setPersistenceRequired();
            }
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (isLead()) {
            level().getEntitiesOfClass(BisonEntity.class,
                            getBoundingBox().inflate(30), b -> b != this && b.isAlive())
                    .stream().findFirst()
                    .ifPresent(b -> b.setLead(true));
        }
        triggerAnim("events", "death");
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) triggerAnim("events", "hurt");
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
                        RawAnimation.begin().thenPlay("animation.bison.death"));
            if (isAggressive() && getDeltaMovement().horizontalDistanceSqr() > 0.003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.bison.sprint"));
            if (getDeltaMovement().horizontalDistanceSqr() > 0.0003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.bison.walk"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.bison.idle"));
        }));
        registrar.add(new AnimationController<>(this, "events", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().thenPlay("animation.bison.attack"))
                .triggerableAnim("hurt",
                        RawAnimation.begin().thenPlay("animation.bison.hurt"))
                .triggerableAnim("death",
                        RawAnimation.begin().thenPlay("animation.bison.death")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}