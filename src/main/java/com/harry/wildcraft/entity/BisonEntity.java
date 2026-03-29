package com.harry.wildcraft.entity;

import com.harry.wildcraft.init.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
import java.util.List;

public class BisonEntity extends Animal implements GeoEntity {

    public static final EntityDataAccessor<Boolean> IS_LEAD =
            SynchedEntityData.defineId(BisonEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> IS_SPRINTING_SYNC =
            SynchedEntityData.defineId(BisonEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final double DETECT_RANGE = 20;

    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.bison.walk");
    private static final RawAnimation SPRINT_ANIM = RawAnimation.begin().thenLoop("animation.bison.sprint");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.bison.idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.bison.attack");

    public BisonEntity(EntityType<? extends BisonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_LEAD, false);
        entityData.define(IS_SPRINTING_SYNC, false);
    }

    public boolean isLead() { return entityData.get(IS_LEAD); }
    public void setLead(boolean value) { entityData.set(IS_LEAD, value); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.FOLLOW_RANGE, DETECT_RANGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));

        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.32, true));

        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));

        targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers());

        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                player -> !((Player) player).isCreative() && !((Player) player).isSpectator()));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        boolean hasTarget = this.getTarget() != null && this.getTarget().isAlive();
        entityData.set(IS_SPRINTING_SYNC, hasTarget);

        if (hasTarget && !isLead() && !existsLeaderNearby()) {
            setLead(true);
        } else if (!hasTarget && isLead()) {
            setLead(false);
        }
    }

    private boolean existsLeaderNearby() {
        List<BisonEntity> list = level().getEntitiesOfClass(
                BisonEntity.class,
                getBoundingBox().inflate(15));

        for (BisonEntity b : list) {
            if (b != this && b.isLead()) return true;
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("events", "attack");
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);

        if (h && src.getEntity() instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) return h;

            List<BisonEntity> list = level().getEntitiesOfClass(
                    BisonEntity.class,
                    getBoundingBox().inflate(50));

            for (BisonEntity b : list) {
                b.setTarget(player);
                b.setLead(false);
            }
            setLead(true);
        }

        return h;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BISON_IDLE.get();
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return ModSounds.BISON_HURT.get();
    }
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BISON_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.7f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {

        registrar.add(new AnimationController<>(this, "main", 5, state -> {

            boolean isMoving = state.isMoving();
            boolean sprint = entityData.get(IS_SPRINTING_SYNC);

            if (isMoving) {
                if (sprint) {
                    return state.setAndContinue(SPRINT_ANIM);
                } else {
                    return state.setAndContinue(WALK_ANIM);
                }
            }

            return state.setAndContinue(IDLE_ANIM);
        }));

        registrar.add(new AnimationController<>(this, "events", 1, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }
}