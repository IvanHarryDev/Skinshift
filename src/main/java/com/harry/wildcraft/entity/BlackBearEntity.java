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

import javax.annotation.Nullable;

public class BlackBearEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public boolean isSleeping = false;
    public boolean isSitting  = false;
    private int sitCooldown   = 200;
    private int sitDuration   = 0;
    private int combatTimer   = 0;
    private int deathTimer    = 0;

    private static final int SIT_COOLDOWN   = 200;
    private static final int SIT_MAX        = 100;
    private static final int COMBAT_TIMEOUT = 200;
    private static final int DEATH_DELAY    = 20;

    public BlackBearEntity(EntityType<? extends BlackBearEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE,  30.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.2, true) {
            @Override public boolean canUse() {
                return !isSleeping && !isSitting && super.canUse();
            }
        });
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 20.0f, 0.7, 0.7,
                e -> !((Player)e).isCreative() && !isAggressive()) {
            @Override public boolean canUse() {
                return !isSleeping && !isSitting && super.canUse();
            }
        });
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override public boolean canUse() {
                return !isSleeping && !isSitting && super.canUse();
            }
        });
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
            if (deathTimer < DEATH_DELAY) setPersistenceRequired();
            return;
        }

        boolean hasTarget = getTarget() != null && getTarget().isAlive();
        if (hasTarget) {
            combatTimer = COMBAT_TIMEOUT;
            isSleeping  = false;
            isSitting   = false;
        } else if (combatTimer > 0) {
            combatTimer--;
        }
        boolean inCombat = combatTimer > 0;

        long dayTime = level().getDayTime() % 24000;
        boolean isNight = dayTime >= 12541 && dayTime <= 23458;
        if (isNight && !inCombat) {
            if (!isSleeping) {
                isSleeping  = true;
                isSitting   = false;
                sitCooldown = SIT_COOLDOWN;
                getNavigation().stop();
            }

            getNavigation().stop();
            return;
        } else {
            isSleeping = false;
        }

        if (!inCombat) {
            if (!isSitting) {
                sitCooldown--;
                if (sitCooldown <= 0) {
                    isSitting   = true;
                    sitDuration = 0;
                    getNavigation().stop();
                }
            } else {
                getNavigation().stop();
                sitDuration++;
                if (sitDuration >= SIT_MAX) {
                    isSitting   = false;
                    sitCooldown = SIT_COOLDOWN;
                }
            }
        } else {
            isSitting   = false;
            sitCooldown = SIT_COOLDOWN;
        }
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) {
            combatTimer = COMBAT_TIMEOUT;
            isSleeping  = false;
            isSitting   = false;
            sitCooldown = SIT_COOLDOWN;
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

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int loot, boolean recent) {
        int amount = 1 + random.nextInt(3);
        spawnAtLocation(new ItemStack(Items.MUTTON, amount));
    }

    @Override
    public void die(DamageSource src) { super.die(src); }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            boolean moving = getDeltaMovement().horizontalDistanceSqr() > 0.001
                    || getNavigation().isInProgress();
            if (!isAlive())
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.death"));
            if (isSleeping)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.sleep"));
            if (isSitting)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.sit"));
            if (combatTimer > 0 && moving)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.sprint"));
            if (moving)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.black_bear.walk"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.black_bear.idle"));
        }));
        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().thenPlay("animation.black_bear.attack"))
                .triggerableAnim("hurt",
                        RawAnimation.begin().thenPlay("animation.black_bear.hurt")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}