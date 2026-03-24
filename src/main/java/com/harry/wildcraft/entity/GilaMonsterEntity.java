package com.harry.wildcraft.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

public class GilaMonsterEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int runTimer = 0;
    private static final int RUN_DURATION = 60;
    private int deathTimer = 0;
    private static final int DEATH_DELAY_TICKS = 20;

    public GilaMonsterEntity(EntityType<? extends GilaMonsterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,     6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE,  2.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 4.0f));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                e -> !((Player)e).isCreative() && e.distanceTo(this) <= 1.5));
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

        if (runTimer > 0) {
            runTimer--;
            if (runTimer == 0 && getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.18);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("events", "attack");
            if (target instanceof LivingEntity le)
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) {
            triggerAnim("events", "hurt");
            if (src.getEntity() instanceof Player p && !p.isCreative()) {
                p.hurt(level().damageSources().mobAttack(this), 2.0f);
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                setTarget(p);
                runTimer = RUN_DURATION;
                if (getAttribute(Attributes.MOVEMENT_SPEED) != null)
                    getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30);
            }
        }
        return h;
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        triggerAnim("events", "death");
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
                        RawAnimation.begin().thenPlay("animation.gila_monster.death"));
            if (runTimer > 0 && getDeltaMovement().horizontalDistanceSqr() > 0.0003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.gila_monster.run"));
            if (getDeltaMovement().horizontalDistanceSqr() > 0.0003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.gila_monster.walk"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.gila_monster.idle"));
        }));
        registrar.add(new AnimationController<>(this, "events", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().thenPlay("animation.gila_monster.attack"))
                .triggerableAnim("hurt",
                        RawAnimation.begin().thenPlay("animation.gila_monster.hurt"))
                .triggerableAnim("death",
                        RawAnimation.begin().thenPlay("animation.gila_monster.death")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
