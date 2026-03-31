package com.harry.wildcraft.entity;

import com.harry.wildcraft.init.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

public class BlackBearEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final EntityDataAccessor<Boolean> IS_SLEEPING_SYNC =
            SynchedEntityData.defineId(BlackBearEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_SITTING_SYNC =
            SynchedEntityData.defineId(BlackBearEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_SPRINTING_SYNC =
            SynchedEntityData.defineId(BlackBearEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_DYING_SYNC =
            SynchedEntityData.defineId(BlackBearEntity.class, EntityDataSerializers.BOOLEAN);

    public boolean isSleeping = false;
    public boolean isSitting  = false;
    private int sitCooldown   = 800;
    private int sitDuration   = 0;
    private int combatTimer   = 0;

    private static final int SIT_COOLDOWN   = 800 + (int)(Math.random() * 400);
    private static final int SIT_MAX        = 150;
    private static final int COMBAT_TIMEOUT = 200;

    private boolean lootDropped = false;
    private DamageSource deathDamageSource = null;

    public BlackBearEntity(EntityType<? extends BlackBearEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0f);
    }

    public boolean isDying() { return entityData.get(IS_DYING_SYNC); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE,  30.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override public boolean canUse() { return !isSleeping && !isSitting && super.canUse(); }
        });
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 20.0f, 0.7, 0.7,
                e -> !((Player)e).isCreative() && !isAggressive()) {
            @Override public boolean canUse() { return !isSleeping && !isSitting && super.canUse(); }
        });
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override public boolean canUse() { return !isSleeping && !isSitting && super.canUse(); }
        });
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                e -> !((Player)e).isCreative() && e.distanceTo(this) <= 10.0));
    }

    // ==================== DEATH SYSTEM ====================
    @Override
    public void die(DamageSource src) {
        if (isDying()) return;
        entityData.set(IS_DYING_SYNC, true);
        deathDamageSource = src;
        setHealth(1f);
        setTarget(null);
        getNavigation().stop();
        setNoAi(true);
        setDeltaMovement(0, 0, 0);
    }

    @Override protected void tickDeath() { }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        if (isDying()) return false;
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

    @Override public boolean isAlive() { return isDying() || super.isAlive(); }
    @Override public boolean isDeadOrDying() { return isDying() ? false : super.isDeadOrDying(); }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isDying() && !level().isClientSide && level() instanceof ServerLevel sl) {
            if (!lootDropped) {
                lootDropped = true;
                dropLootFromTable(sl, player);
            }
            discard();
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void dropLootFromTable(ServerLevel sl, Player player) {
        LootTable lootTable = sl.getServer().getLootData()
                .getLootTable(getType().getDefaultLootTable());
        LootParams.Builder builder = new LootParams.Builder(sl)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.DAMAGE_SOURCE,
                        deathDamageSource != null ? deathDamageSource : sl.damageSources().generic());
        if (deathDamageSource != null && deathDamageSource.getEntity() instanceof Player killer) {
            builder.withParameter(LootContextParams.KILLER_ENTITY, killer)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
        } else {
            builder.withParameter(LootContextParams.KILLER_ENTITY, player)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
        }
        LootParams params = builder.create(LootContextParamSets.ENTITY);
        List<ItemStack> drops = lootTable.getRandomItems(params);
        for (ItemStack stack : drops) spawnAtLocation(stack);
        if (getExperienceReward() > 0)
            net.minecraft.world.entity.ExperienceOrb.award(sl, position(), getExperienceReward());
    }

    @Override public boolean isPickable() { return isDying() || super.isPickable(); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_SLEEPING_SYNC, false);
        entityData.define(IS_SITTING_SYNC, false);
        entityData.define(IS_SPRINTING_SYNC, false);
        entityData.define(IS_DYING_SYNC, false);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("events", "attack");
        return hit;
    }

    // ==================== TICK ====================
    @Override
    public void tick() {
        if (isDying()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            baseTick();
            return;
        }

        super.tick();
        if (level().isClientSide) return;

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
        } else {
            isSleeping = false;
        }

        if (!inCombat && !isSleeping) {
            if (!isSitting) {
                sitCooldown--;
                if (sitCooldown <= 0 && !this.isInWater()) {
                    isSitting   = true;
                    sitDuration = 0;
                    getNavigation().stop();
                } else if (sitCooldown <= 0 && this.isInWater()) {
                    sitCooldown = 200;
                }
            } else {
                getNavigation().stop();
                sitDuration++;
                if (sitDuration >= SIT_MAX || this.isInWater()) {
                    isSitting   = false;
                    sitCooldown = SIT_COOLDOWN;
                }
            }
        } else if (inCombat) {
            isSitting   = false;
            sitCooldown = SIT_COOLDOWN;
        }

        entityData.set(IS_SLEEPING_SYNC, isSleeping);
        entityData.set(IS_SITTING_SYNC, isSitting);
        entityData.set(IS_SPRINTING_SYNC, inCombat);
    }

    // ==================== SPAWNING ====================
    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable net.minecraft.nbt.CompoundTag tag) {
        spawnData = super.finalizeSpawn(level, diff, spawnType, spawnData, tag);
        if (spawnType != MobSpawnType.SPAWNER && spawnType != MobSpawnType.SPAWN_EGG
                && spawnType != MobSpawnType.CHUNK_GENERATION && spawnType != MobSpawnType.STRUCTURE
                && level instanceof ServerLevel sl) {
            int extra = random.nextInt(100) < 85 ? 1 : 2;
            for (int i = 0; i < extra; i++) {
                BlackBearEntity companion = new BlackBearEntity(
                        com.harry.wildcraft.init.ModEntities.BLACK_BEAR.get(), sl);
                double nx = getX() + (random.nextDouble() - 0.5) * 10;
                double nz = getZ() + (random.nextDouble() - 0.5) * 10;
                if (sl.hasChunk((int)nx >> 4, (int)nz >> 4)) {
                    companion.moveTo(nx, getY(), nz, random.nextFloat() * 360, 0);
                    sl.addFreshEntity(companion);
                }
            }
        }
        return spawnData;
    }

    // ==================== SOUNDS ====================
    @Override protected SoundEvent getAmbientSound() { return isDying() ? null : ModSounds.BLACK_BEAR_IDLE.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.BLACK_BEAR_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.BLACK_BEAR_DEATH.get(); }
    @Override protected float getSoundVolume() { return 0.35f; }
    @Override public int getAmbientSoundInterval() { return 500; }

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // ==================== GECKOLIB ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (isDying())
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.black_bear.death"));
            if (entityData.get(IS_SLEEPING_SYNC))
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.black_bear.sleep"));
            if (entityData.get(IS_SITTING_SYNC))
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.black_bear.sit"));

            boolean moving = state.isMoving();
            if (entityData.get(IS_SPRINTING_SYNC) && moving)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.black_bear.sprint"));
            if (moving)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.black_bear.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.black_bear.idle"));
        }));

        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.black_bear.attack"))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay("animation.black_bear.hurt")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}