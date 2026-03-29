package com.harry.wildcraft.entity;

import com.harry.wildcraft.init.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
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

    private int combatTimer = 0;
    private static final int COMBAT_TIMEOUT = 100; // 5 s

    private int deathTimer = 0;
    private static final int DEATH_DELAY = 20;

    public GilaMonsterEntity(EntityType<? extends GilaMonsterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,     6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE,  2.0)
                .add(Attributes.FOLLOW_RANGE,  16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 4.0f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
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
            if (deathTimer < DEATH_DELAY) setPersistenceRequired();
            return;
        }

        boolean hasTarget = getTarget() != null && getTarget().isAlive();
        if (hasTarget) {
            combatTimer = COMBAT_TIMEOUT;
        } else if (combatTimer > 0) {
            combatTimer--;
        }

        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED)
                    .setBaseValue(combatTimer > 0 ? 0.30 : 0.18);
        }

        if (getTarget() instanceof Player p && distanceTo(p) > 15)
            setTarget(null);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            playSound(ModSounds.GILA_MONSTER_ATTACK.get(), 1.0f, 1.0f);
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
            combatTimer = COMBAT_TIMEOUT;
            if (src.getEntity() instanceof Player p && !p.isCreative()) {
                p.hurt(level().damageSources().mobAttack(this), 2.0f);
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                setTarget(p);
            }
        }
        return h;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GILA_MONSTER_IDLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GILA_MONSTER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.25f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
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
            if (!isAlive())
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.gila_monster.death"));

            boolean moving = state.isMoving();

            boolean isRunning = this.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.2;

            if (moving) {
                if (isRunning) {
                    return state.setAndContinue(
                            RawAnimation.begin().thenLoop("animation.gila_monster.run"));
                } else {
                    return state.setAndContinue(
                            RawAnimation.begin().thenLoop("animation.gila_monster.walk"));
                }
            }

            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.gila_monster.idle"));
        }));

        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().thenPlay("animation.gila_monster.attack"))
                .triggerableAnim("hurt",
                        RawAnimation.begin().thenPlay("animation.gila_monster.hurt")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}