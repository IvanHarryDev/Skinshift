package com.harry.wildcraft.entity;

import com.harry.wildcraft.init.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

public class BisonEntity extends Animal implements GeoEntity {

    public static final EntityDataAccessor<Boolean> IS_LEAD =
            SynchedEntityData.defineId(BisonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_SPRINTING_SYNC =
            SynchedEntityData.defineId(BisonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> ATTACK_TIMER_SYNC =
            SynchedEntityData.defineId(BisonEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_DYING_SYNC =
            SynchedEntityData.defineId(BisonEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final double DETECT_RANGE = 20;

    private int combatTimer = 0;
    private static final int COMBAT_TIMEOUT = 200;
    private static final int ATTACK_ANIM_DURATION = 12;
    private boolean clientWasAttacking = false;

    // ---- Death system ----
    private boolean lootDropped = false;
    private DamageSource deathDamageSource = null;

    private static final RawAnimation WALK_ANIM   = RawAnimation.begin().thenLoop("animation.bison.walk");
    private static final RawAnimation SPRINT_ANIM = RawAnimation.begin().thenLoop("animation.bison.sprint");
    private static final RawAnimation IDLE_ANIM   = RawAnimation.begin().thenLoop("animation.bison.idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.bison.attack");
    private static final RawAnimation DEATH_ANIM  = RawAnimation.begin().thenPlay("animation.bison.death");

    public BisonEntity(EntityType<? extends BisonEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0f);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_LEAD, false);
        entityData.define(IS_SPRINTING_SYNC, false);
        entityData.define(ATTACK_TIMER_SYNC, 0);
        entityData.define(IS_DYING_SYNC, false);
    }

    public boolean isDying() { return entityData.get(IS_DYING_SYNC); }

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

    @Override
    protected void tickDeath() { }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        if (isDying()) return false;
        return super.hurt(src, dmg);
    }

    @Override
    public boolean isAlive() {
        if (isDying()) return true;
        return super.isAlive();
    }

    @Override
    public boolean isDeadOrDying() {
        if (isDying()) return false;
        return super.isDeadOrDying();
    }

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

    @Override
    public boolean isPickable() {
        return isDying() || super.isPickable();
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
        if (hasTarget) combatTimer = COMBAT_TIMEOUT;
        else if (combatTimer > 0) combatTimer--;
        boolean inCombat = combatTimer > 0;

        int atk = entityData.get(ATTACK_TIMER_SYNC);
        if (atk > 0) entityData.set(ATTACK_TIMER_SYNC, atk - 1);

        entityData.set(IS_SPRINTING_SYNC, inCombat);

        if (hasTarget && !isLead() && !existsLeaderNearby()) setLead(true);
        else if (!hasTarget && isLead()) setLead(false);
    }

    private boolean existsLeaderNearby() {
        List<BisonEntity> list = level().getEntitiesOfClass(
                BisonEntity.class, getBoundingBox().inflate(15));
        for (BisonEntity b : list) {
            if (b != this && b.isLead()) return true;
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) entityData.set(ATTACK_TIMER_SYNC, ATTACK_ANIM_DURATION);
        return hit;
    }

    // ==================== SOUNDS ====================
    @Override protected SoundEvent getAmbientSound() { return isDying() ? null : ModSounds.BISON_IDLE.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.BISON_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.BISON_DEATH.get(); }
    @Override protected float getSoundVolume() { return 0.4f; }
    @Override public int getAmbientSoundInterval() { return 600; }

    // ==================== GECKOLIB ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (isDying()) return state.setAndContinue(DEATH_ANIM);

            int atkTimer = entityData.get(ATTACK_TIMER_SYNC);
            boolean isAttacking = atkTimer > 0;
            boolean moving = state.isMoving();
            boolean sprint = entityData.get(IS_SPRINTING_SYNC);

            if (isAttacking) {
                if (!clientWasAttacking) {
                    clientWasAttacking = true;
                    state.getController().setAnimation(ATTACK_ANIM);
                }
                return PlayState.CONTINUE;
            }
            clientWasAttacking = false;
            if (sprint && moving) return state.setAndContinue(SPRINT_ANIM);
            if (moving) return state.setAndContinue(WALK_ANIM);
            return state.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
}