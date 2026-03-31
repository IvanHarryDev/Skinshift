package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerAggressiveGoal;
import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerBreakBlocksGoal;
import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerDragPlayerGoal;
import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerFollowPlayerGoal;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class SkinwalkerEntity extends Monster implements GeoEntity {

    public static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> MORPHED_INTO =
            SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_MORPHED =
            SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_SCREAMING =
            SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID    targetPlayerUUID;
    private int     modeTimer             = 0;
    private boolean isDraggingPlayer      = false;
    private boolean modeLocked            = false;
    private boolean pendingThreateningHit = false;
    private String  lastBiome             = "";
    private int     lookAtTimer           = 0;
    private boolean nocturnalCrouch       = false;
    private boolean isEating              = false;
    private boolean isRangedHurt          = false;
    private boolean isWeakAttacking       = false;
    private boolean isStrongAttacking     = false;
    private boolean isMorphing            = false;

    public static final int PASSIVE_DURATION     = 24000 * 3;
    public static final int THREATENING_DURATION  = 24000 * 2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SkinwalkerEntity(EntityType<? extends SkinwalkerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,          200.0)
                .add(Attributes.MOVEMENT_SPEED,        0.3)
                .add(Attributes.ATTACK_DAMAGE,          2.0)
                .add(Attributes.FOLLOW_RANGE,         256.0)
                .add(Attributes.KNOCKBACK_RESISTANCE,   1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MODE,         SkinwalkerMode.PASSIVE.id);
        entityData.define(MORPHED_INTO, "none");
        entityData.define(IS_MORPHED,   false);
        entityData.define(IS_SCREAMING, false);
    }

    // ─── Goals ───
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new SkinwalkerDragPlayerGoal(this));
        goalSelector.addGoal(1, new SkinwalkerAggressiveGoal(this));
        goalSelector.addGoal(2, new SkinwalkerBreakBlocksGoal(this));
        goalSelector.addGoal(3, new SkinwalkerFollowPlayerGoal(this));
    }

    // ─── Tick ───
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || modeLocked) return;
        if (targetPlayerUUID == null) return;

        modeTimer++;
        SkinwalkerMode mode = getMode();
        if (mode == SkinwalkerMode.PASSIVE && modeTimer >= PASSIVE_DURATION) {
            setMode(SkinwalkerMode.THREATENING);
            modeTimer = 0;
        } else if (mode == SkinwalkerMode.THREATENING && modeTimer >= THREATENING_DURATION) {
            setMode(SkinwalkerMode.AGGRESSIVE);
            modeTimer = 0;
        }
    }

    // ─── Hurt logic ───
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;

        SkinwalkerMode mode    = getMode();
        boolean isProjectile   = source.getDirectEntity() instanceof Projectile;
        boolean isMeleePlayer  = source.getEntity() instanceof Player && !isProjectile;

        // ── PASSIVE ──
        if (mode == SkinwalkerMode.PASSIVE) {
            if (source.getEntity() instanceof Player p) {
                poofAndRespawn((ServerLevel) level(), p);
            }
            return false;
        }

        // ── THREATENING ──
        if (mode == SkinwalkerMode.THREATENING) {
            if (source.getEntity() instanceof Player p) {
                setWeakAttacking(true);
                p.hurt(level().damageSources().mobAttack(this), 2.0f);
                poofAndRespawn((ServerLevel) level(), p);
            }
            return false;
        }

        // ── AGGRESSIVE ──
        if (isMorphed() && isMeleePlayer) {
            setMorphed(false);
            setMorphedInto("none");
        }

        if (isProjectile && isDraggingPlayer) {
            isDraggingPlayer = false;
            setScreaming(true);
            pendingThreateningHit = true;
            return false;
        }

        if (isProjectile) {
            setRangedHurt(true);
            setScreaming(true);
        }

        return super.hurt(source, amount);
    }

    // ─── Poof and respawn out of sight ───
    public void poofAndRespawn(ServerLevel level, Player target) {
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                getX(), getY() + 1, getZ(), 30, 0.5, 0.8, 0.5, 0.05
        );

        Vec3 newPos = SightHelper.findPositionOutOfSight(
                level, (ServerPlayer) target, 100, 15
        );
        if (newPos != null) {
            teleportTo(newPos.x, newPos.y, newPos.z);
            SkinwalkerMorphHelper.morphToClosestBiomeAnimal(this, level, newPos);
            lookAtTimer = 0;
        }
    }

    // ─── Persistence ───
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SWMode", getMode().id);
        tag.putInt("SWModeTimer", modeTimer);
        tag.putBoolean("SWModeLocked", modeLocked);
        tag.putString("SWLastBiome", lastBiome);
        tag.putString("SWMorphedInto", getMorphedInto());
        tag.putBoolean("SWIsMorphed", isMorphed());
        if (targetPlayerUUID != null) {
            tag.putUUID("SWTargetPlayer", targetPlayerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SWMode"))       setMode(SkinwalkerMode.fromId(tag.getInt("SWMode")));
        if (tag.contains("SWModeTimer"))  modeTimer  = tag.getInt("SWModeTimer");
        if (tag.contains("SWModeLocked")) modeLocked = tag.getBoolean("SWModeLocked");
        if (tag.contains("SWLastBiome"))  lastBiome  = tag.getString("SWLastBiome");
        if (tag.contains("SWMorphedInto")) setMorphedInto(tag.getString("SWMorphedInto"));
        if (tag.contains("SWIsMorphed"))  setMorphed(tag.getBoolean("SWIsMorphed"));
        if (tag.hasUUID("SWTargetPlayer")) targetPlayerUUID = tag.getUUID("SWTargetPlayer");
    }

    // ─── Getters / Setters ───
    public SkinwalkerMode getMode()                  { return SkinwalkerMode.fromId(entityData.get(MODE)); }
    public void           setMode(SkinwalkerMode m)  { entityData.set(MODE, m.id); }
    public void           setModeLocked(boolean b)   { this.modeLocked = b; }
    public boolean        isModeLocked()             { return modeLocked; }
    public UUID           getTargetPlayerUUID()      { return targetPlayerUUID; }
    public void           setTargetPlayer(UUID uuid) { this.targetPlayerUUID = uuid; }
    public boolean        isDraggingPlayer()         { return isDraggingPlayer; }
    public void           setDraggingPlayer(boolean b){ this.isDraggingPlayer = b; }
    public boolean        isPendingThreateningHit()  { return pendingThreateningHit; }
    public void           setPendingThreateningHit(boolean b){ this.pendingThreateningHit = b; }
    public String         getLastBiome()             { return lastBiome; }
    public void           setLastBiome(String b)     { this.lastBiome = b; }
    public int            getLookAtTimer()            { return lookAtTimer; }
    public void           setLookAtTimer(int v)      { this.lookAtTimer = v; }
    public void           incrementLookAtTimer()     { this.lookAtTimer++; }

    public boolean isNocturnalCrouch()       { return nocturnalCrouch; }
    public void    randomizeNocturnalCrouch(){ this.nocturnalCrouch = this.random.nextBoolean(); }

    public boolean isEating()                { return isEating; }
    public void    setEating(boolean b)      { this.isEating = b; }

    public boolean isRangedHurt()            { return isRangedHurt; }
    public void    setRangedHurt(boolean b)  { this.isRangedHurt = b; }

    public boolean isWeakAttacking()         { return isWeakAttacking; }
    public void    setWeakAttacking(boolean b){ this.isWeakAttacking = b; }

    public boolean isStrongAttacking()       { return isStrongAttacking; }
    public void    setStrongAttacking(boolean b){ this.isStrongAttacking = b; }

    public boolean isMorphing()              { return isMorphing; }
    public void    setMorphing(boolean b)    { this.isMorphing = b; }

    public boolean isMorphed()               { return entityData.get(IS_MORPHED); }
    public void    setMorphed(boolean v)     { entityData.set(IS_MORPHED, v); }
    public String  getMorphedInto()          { return entityData.get(MORPHED_INTO); }
    public void    setMorphedInto(String v)  { entityData.set(MORPHED_INTO, v); }
    public boolean isScreaming()             { return entityData.get(IS_SCREAMING); }
    public void    setScreaming(boolean v)   { entityData.set(IS_SCREAMING, v); }

    public int     getModeTimer()            { return modeTimer; }
    public void    setModeTimer(int v)       { this.modeTimer = v; }

    // ─── Sound ───
    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }
    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    // ─── GeckoLib ───
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 5, state -> {
            SkinwalkerMode mode = getMode();

            // ── Morph (one-shot, highest priority — texture stays until done) ──
            if (isMorphing()) {
                if (state.getController().getAnimationState() == AnimationController.State.STOPPED)
                    setMorphing(false);
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.skinwalker.morph"));
            }

            // ── Scream (one-shot) ──
            if (isScreaming()) {
                if (state.getController().getAnimationState() == AnimationController.State.STOPPED)
                    setScreaming(false);
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.skinwalker.scream"));
            }

            // ── Ranged hurt (one-shot) ──
            if (isRangedHurt()) {
                if (state.getController().getAnimationState() == AnimationController.State.STOPPED)
                    setRangedHurt(false);
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.skinwalker.ranged_hurt"));
            }

            // ── Weak attack (one-shot, threatening) ──
            if (isWeakAttacking()) {
                if (state.getController().getAnimationState() == AnimationController.State.STOPPED)
                    setWeakAttacking(false);
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.skinwalker.weak_attack"));
            }

            // ── Strong attack (one-shot, aggressive catch) ──
            if (isStrongAttacking()) {
                if (state.getController().getAnimationState() == AnimationController.State.STOPPED)
                    setStrongAttacking(false);
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.skinwalker.strong_attack"));
            }

            // ── Catch → Drag (aggressive) ──
            if (isDraggingPlayer()) {
                return state.setAndContinue(
                        RawAnimation.begin()
                                .thenPlay("animation.skinwalker.catch_to_drag")
                                .thenLoop("animation.skinwalker.drag"));
            }

            // ── Eat (one-shot after drag) ──
            if (isEating()) {
                if (state.getController().getAnimationState() == AnimationController.State.STOPPED)
                    setEating(false);
                return state.setAndContinue(
                        RawAnimation.begin().thenPlay("animation.skinwalker.eat"));
            }

            // ── Nocturnal crouch/look_around (threatening, nighttime, stationary) ──
            if (mode == SkinwalkerMode.THREATENING && !level().isDay()
                    && getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                if (isNocturnalCrouch()) {
                    return state.setAndContinue(
                            RawAnimation.begin().thenLoop("animation.skinwalker.crouch"));
                } else {
                    return state.setAndContinue(
                            RawAnimation.begin().thenLoop("animation.skinwalker.look_around"));
                }
            }

            // ── Sprint (aggressive, moving) ──
            if (mode == SkinwalkerMode.AGGRESSIVE
                    && getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.skinwalker.sprint"));
            }

            // ── Walk ──
            if (getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.skinwalker.walk"));
            }

            // ── Idle ──
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.skinwalker.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}