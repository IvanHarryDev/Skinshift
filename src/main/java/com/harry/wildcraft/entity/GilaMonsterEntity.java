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

public class GilaMonsterEntity extends Animal implements GeoEntity {

    public static final EntityDataAccessor<Boolean> IS_DYING_SYNC =
            SynchedEntityData.defineId(GilaMonsterEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int combatTimer = 0;
    private static final int COMBAT_TIMEOUT = 100;

    private boolean lootDropped = false;
    private DamageSource deathDamageSource = null;

    public GilaMonsterEntity(EntityType<? extends GilaMonsterEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0f);
    }

    public boolean isDying() { return entityData.get(IS_DYING_SYNC); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_DYING_SYNC, false);
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
    @Override public boolean isAlive() { return isDying() || super.isAlive(); }
    @Override public boolean isDeadOrDying() { return isDying() ? false : super.isDeadOrDying(); }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        if (isDying()) return false;
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

        if (getAttribute(Attributes.MOVEMENT_SPEED) != null)
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(combatTimer > 0 ? 0.30 : 0.18);

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

    // ==================== SOUNDS ====================
    @Override protected SoundEvent getAmbientSound() { return isDying() ? null : ModSounds.GILA_MONSTER_IDLE.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.GILA_MONSTER_DEATH.get(); }
    @Override protected float getSoundVolume() { return 0.15f; }
    @Override public int getAmbientSoundInterval() { return 800; }

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // ==================== GECKOLIB ====================
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (isDying())
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.gila_monster.death"));

            boolean moving = state.isMoving();
            boolean isRunning = this.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.2;

            if (moving) {
                if (isRunning)
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.gila_monster.run"));
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.gila_monster.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.gila_monster.idle"));
        }));

        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.gila_monster.attack"))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay("animation.gila_monster.hurt")));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}