package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.CoyotePackAttackGoal;
import com.harry.wildcraft.entity.goal.CoyoteStalkGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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

public class CoyoteEntity extends Animal implements GeoEntity {

    public enum PackState { IDLE, STALKING, ATTACKING }
    private PackState packState = PackState.IDLE;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int howlTimer = 0;
    private static final int HOWL_INTERVAL = 600;
    private int deathTimer = 0;
    private static final int DEATH_DELAY_TICKS = 20;

    public CoyoteEntity(EntityType<? extends CoyoteEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE,  4.0)
                .add(Attributes.FOLLOW_RANGE,  150.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(1, new CoyoteStalkGoal(this));
        goalSelector.addGoal(2, new CoyotePackAttackGoal(this));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false,
                e -> !((Player)e).isCreative() && !((Player)e).isSpectator()));
    }

    public PackState getPackState() { return packState; }
    public void setPackState(PackState s) { this.packState = s; }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        if (!isAlive()) {
            deathTimer++;
            if (deathTimer < DEATH_DELAY_TICKS) setPersistenceRequired();
            return;
        }

        if (packState == PackState.ATTACKING && getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
        } else {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
        }

        if (getTarget() instanceof Player p && packState == PackState.ATTACKING
                && distanceTo(p) > 150) {
            setTarget(null);
            packState = PackState.IDLE;
        }

        howlTimer++;
        boolean hasNearbyTarget = level().getNearestPlayer(this, 30) != null;
        if (howlTimer >= HOWL_INTERVAL || (hasNearbyTarget && howlTimer > 60)) {
            if (packState == PackState.IDLE || packState == PackState.STALKING) {
                triggerAnim("events", "howl");
                howlTimer = 0;
            }
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
            packState = PackState.ATTACKING;
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
                        RawAnimation.begin().thenPlay("animation.coyote.death"));
            if (packState == PackState.ATTACKING
                    && getDeltaMovement().horizontalDistanceSqr() > 0.003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.sprint"));
            if (packState == PackState.STALKING
                    && getDeltaMovement().horizontalDistanceSqr() > 0.0003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.stalk"));
            if (getDeltaMovement().horizontalDistanceSqr() > 0.0003)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.walk"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.coyote.idle"));
        }));
        registrar.add(new AnimationController<>(this, "events", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().thenPlay("animation.coyote.attack"))
                .triggerableAnim("hurt",
                        RawAnimation.begin().thenPlay("animation.coyote.hurt"))
                .triggerableAnim("howl",
                        RawAnimation.begin().thenPlay("animation.coyote.howl"))
                .triggerableAnim("death",
                        RawAnimation.begin().thenPlay("animation.coyote.death")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}