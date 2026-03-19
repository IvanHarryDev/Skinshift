package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.DeerJumpGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class DeerEntity extends Animal implements GeoEntity {

    public static final EntityDataAccessor<Boolean> IS_MALE =
            SynchedEntityData.defineId(DeerEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    private int counterTimer = 0;

    public DeerEntity(EntityType<? extends DeerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_MALE, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE,  8.0)
                .add(Attributes.JUMP_STRENGTH,  1.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, false));
        goalSelector.addGoal(2, new EatBlockGoal(this));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 10.0f, 1.5, 1.5,
                e -> !((Player) e).isCreative()));
        goalSelector.addGoal(4, new DeerJumpGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (counterTimer > 0 && --counterTimer == 0) setTarget(null);
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean hurt = super.hurt(src, dmg);
        if (hurt && src.getEntity() instanceof LivingEntity le) {
            setTarget(le);
            counterTimer = 600;
        }
        return hurt;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        DifficultyInstance diff, MobSpawnType type,
                                        SpawnGroupData data, CompoundTag tag) {
        super.finalizeSpawn(level, diff, type, data, tag);
        entityData.set(IS_MALE, data == null || random.nextBoolean());
        return data == null ? new AgeableMob.AgeableMobGroupData(false) : data;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        r.add(new AnimationController<>(this, "main", 5, state -> {
            if (isAggressive())
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.deer.run"));
            if (getDeltaMovement().horizontalDistanceSqr() > 0.001)
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.deer.walk"));
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.deer.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
