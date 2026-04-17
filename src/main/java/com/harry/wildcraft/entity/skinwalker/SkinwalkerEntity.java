package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerBreakBlocksGoal;
import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerFollowPlayerGoal;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
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

    public static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> MORPHED_INTO = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_MORPHED = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_SCREAMING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_MORPHING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_MELEE_HURT = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_RANGED_HURT = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_WEAK_ATTACKING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_STRONG_ATTACKING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_DRAGGING = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_CROUCHING_ANIM = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_LOOKING_AROUND = SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID targetPlayerUUID;
    private int modeTimer = 0;
    private boolean modeLocked = false;
    private boolean pendingThreateningHit = false;
    private String lastBiome = "";
    private int lookAtTimer = 0;
    private boolean nocturnalCrouch = false;
    private int morphTimer = -1;
    private String pendingMorphTarget = null;
    private boolean pendingMorphIsOwl = false;
    private Mob currentDecoy = null;
    private String currentDecoyType = "none";
    private Vec3 lockedPosition = null;

    private final SkinwalkerCombatManager combatManager = new SkinwalkerCombatManager();

    private static final EntityDimensions NORMAL_DIMENSIONS = EntityDimensions.scalable(0.6f, 2.4f);
    private static final EntityDimensions MORPHED_DIMENSIONS = EntityDimensions.scalable(0.01f, 0.01f);
    public static final int MORPH_ANIM_TICKS = 40;
    public static final int PASSIVE_DURATION = 24000 * 3;
    public static final int THREATENING_DURATION = 24000 * 2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SkinwalkerEntity(EntityType<? extends SkinwalkerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.FOLLOW_RANGE, 256)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    public SkinwalkerCombatManager getCombatManager() {
        return combatManager;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MODE, SkinwalkerMode.PASSIVE.id);
        entityData.define(MORPHED_INTO, "none");
        entityData.define(IS_MORPHED, false);
        entityData.define(IS_SCREAMING, false);
        entityData.define(IS_MORPHING, false);
        entityData.define(IS_EATING, false);
        entityData.define(IS_MELEE_HURT, false);
        entityData.define(IS_RANGED_HURT, false);
        entityData.define(IS_WEAK_ATTACKING, false);
        entityData.define(IS_STRONG_ATTACKING, false);
        entityData.define(IS_DRAGGING, false);
        entityData.define(IS_CROUCHING_ANIM, false);
        entityData.define(IS_LOOKING_AROUND, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SkinwalkerBreakBlocksGoal(this));
        goalSelector.addGoal(3, new SkinwalkerFollowPlayerGoal(this));
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (isMorphed() && !isMorphing()) return MORPHED_DIMENSIONS;
        return NORMAL_DIMENSIONS;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (IS_MORPHED.equals(key) || IS_MORPHING.equals(key)) refreshDimensions();
    }

    public void lockPosition() {
        lockedPosition = position();
    }

    public void unlockPosition() {
        lockedPosition = null;
    }

    // ═══════════════════════════════════════════════════════════
    //  TICK
    // ═══════════════════════════════════════════════════════════
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        // Position lock
        if (lockedPosition != null) {
            setPos(lockedPosition.x, lockedPosition.y, lockedPosition.z);
            setDeltaMovement(0, 0, 0);
        }

        // Morph timer
        if (morphTimer >= 0) {
            morphTimer++;
            if (lockedPosition == null) lockPosition();
            if (morphTimer >= MORPH_ANIM_TICKS) {
                applyPendingMorph();
                unlockPosition();
            }
        }

        // Decoy
        handleDecoy();

        // ═══ COMBAT MANAGER ═══
        if (level() instanceof ServerLevel sl && getMode() == SkinwalkerMode.AGGRESSIVE
                && targetPlayerUUID != null) {
            if (combatManager.getPhase() == SkinwalkerCombatManager.Phase.INACTIVE) {
                combatManager.start(this);
            }
            combatManager.tick(this, sl);
            return;
        }

        if (combatManager.getPhase() != SkinwalkerCombatManager.Phase.INACTIVE
                && getMode() != SkinwalkerMode.AGGRESSIVE) {
            combatManager.stop(this);
        }

        if (modeLocked || targetPlayerUUID == null) return;
        if (level() instanceof ServerLevel sl2) {
            Player t = sl2.getPlayerByUUID(targetPlayerUUID);
            if (t != null && (t.isCreative() || t.isSpectator())) return;
        }
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

    private void handleDecoy() {
        if (!(level() instanceof ServerLevel sl)) return;
        String mi = getMorphedInto();
        boolean need = isMorphed() && !mi.equals("none") && !isMorphing();
        if (need) {
            if (currentDecoy == null || !currentDecoy.isAlive() || !currentDecoyType.equals(mi)) {
                SkinwalkerDecoyHelper.removeDecoy(currentDecoy);
                EntityType<?> dt = resolveEntityType(mi);
                if (dt != null) {
                    currentDecoy = SkinwalkerDecoyHelper.createDecoy(this, sl, dt);
                    currentDecoyType = mi;
                }
            }
            SkinwalkerDecoyHelper.tickDecoy(this, currentDecoy);
        } else if (currentDecoy != null) {
            SkinwalkerDecoyHelper.removeDecoy(currentDecoy);
            currentDecoy = null;
            currentDecoyType = "none";
        }
    }

    private EntityType<?> resolveEntityType(String id) {
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE)
            if (t.getDescriptionId().equals(id)) return t;
        return null;
    }

    // Kill
    @Override
    public void remove(RemovalReason r) {
        cleanupDecoy();
        super.remove(r);
    }

    @Override
    public void kill() {
        cleanupDecoy();
        super.kill();
    }

    public void forceKill() {
        cleanupDecoy();
        unlockPosition();
        combatManager.stop(this);
        setHealth(0);
        discard();
    }

    private void cleanupDecoy() {
        if (currentDecoy != null) {
            SkinwalkerDecoyHelper.removeDecoy(currentDecoy);
            currentDecoy = null;
            currentDecoyType = "none";
        }
    }

    public Mob getCurrentDecoy() {
        return currentDecoy;
    }

    // Morph
    public void startMorph(String t, boolean owl) {
        if (morphTimer >= 0) applyPendingMorph();
        cleanupDecoy();
        pendingMorphTarget = t;
        pendingMorphIsOwl = owl;
        morphTimer = 0;
        setMorphing(true);
        getNavigation().stop();
        lockPosition();
    }

    public void morphInstant(String t, boolean owl) {
        if (morphTimer >= 0) {
            morphTimer = -1;
            pendingMorphTarget = null;
            pendingMorphIsOwl = false;
            setMorphing(false);
        }
        cleanupDecoy();
        unlockPosition();
        if (t.equals("none")) {
            setMorphedInto("none");
            setMorphed(false);
            setNoGravity(false);
        } else {
            setMorphedInto(t);
            setMorphed(true);
            if (owl) setNoGravity(true);
        }
    }

    private void applyPendingMorph() {
        if (pendingMorphTarget == null) {
            morphTimer = -1;
            setMorphing(false);
            return;
        }
        if (pendingMorphTarget.equals("none")) {
            setMorphedInto("none");
            setMorphed(false);
            setNoGravity(false);
        } else {
            setMorphedInto(pendingMorphTarget);
            setMorphed(true);
            if (pendingMorphIsOwl) setNoGravity(true);
        }
        morphTimer = -1;
        pendingMorphTarget = null;
        pendingMorphIsOwl = false;
        setMorphing(false);
    }

    public boolean isMorphInProgress() {
        return morphTimer >= 0;
    }

    // Combat
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        SkinwalkerMode mode = getMode();
        boolean proj = source.getDirectEntity() instanceof Projectile;
        boolean melee = source.getEntity() instanceof Player && !proj;

        if (mode == SkinwalkerMode.PASSIVE) {
            if (source.getEntity() instanceof Player p)
                poofAndRespawn((ServerLevel) level(), p);
            return false;
        }
        if (mode == SkinwalkerMode.THREATENING) {
            if (source.getEntity() instanceof Player p) {
                setWeakAttacking(true);
                p.hurt(level().damageSources().mobAttack(this), 2f);
                poofAndRespawn((ServerLevel) level(), p);
            }
            return false;
        }
        // AGGRESSIVE
        if (proj && isDraggingPlayer()) {
            setDraggingPlayer(false);
            setScreaming(true);
            pendingThreateningHit = true;
            return false;
        }
        if (isMorphed() && melee && !isMorphInProgress())
            startMorph("none", false);
        if (melee) setMeleeHurt(true);
        if (proj) {
            setRangedHurt(true);
            setScreaming(true);
            getNavigation().stop();
        }
        return super.hurt(source, amount);
    }

    public void poofAndRespawn(ServerLevel level, Player target) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                getX(), getY() + 1, getZ(), 30, .5, .8, .5, .05);
        Vec3 p = SightHelper.findPositionOutOfSight(level, (ServerPlayer) target, 100, 15);
        if (p != null) {
            unlockPosition();
            teleportTo(p.x, p.y, p.z);
            SkinwalkerMorphHelper.morphInstantToClosestBiomeAnimal(this, level, p);
            lookAtTimer = 0;
        }
    }

    // Persistence
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("SWMode", getMode().id);
        t.putInt("SWModeTimer", modeTimer);
        t.putBoolean("SWModeLocked", modeLocked);
        t.putString("SWLastBiome", lastBiome);
        t.putString("SWMorphedInto", getMorphedInto());
        t.putBoolean("SWIsMorphed", isMorphed());
        if (targetPlayerUUID != null) t.putUUID("SWTargetPlayer", targetPlayerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("SWMode")) setMode(SkinwalkerMode.fromId(t.getInt("SWMode")));
        if (t.contains("SWModeTimer")) modeTimer = t.getInt("SWModeTimer");
        if (t.contains("SWModeLocked")) modeLocked = t.getBoolean("SWModeLocked");
        if (t.contains("SWLastBiome")) lastBiome = t.getString("SWLastBiome");
        if (t.contains("SWMorphedInto")) setMorphedInto(t.getString("SWMorphedInto"));
        if (t.contains("SWIsMorphed")) setMorphed(t.getBoolean("SWIsMorphed"));
        if (t.hasUUID("SWTargetPlayer")) targetPlayerUUID = t.getUUID("SWTargetPlayer");
    }

    // Getters/Setters
    public SkinwalkerMode getMode() {
        return SkinwalkerMode.fromId(entityData.get(MODE));
    }

    public void setMode(SkinwalkerMode m) {
        entityData.set(MODE, m.id);
    }

    public void setModeLocked(boolean b) {
        modeLocked = b;
    }

    public boolean isModeLocked() {
        return modeLocked;
    }

    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
    }

    public void setTargetPlayer(UUID u) {
        targetPlayerUUID = u;
    }

    public boolean isPendingThreateningHit() {
        return pendingThreateningHit;
    }

    public void setPendingThreateningHit(boolean b) {
        pendingThreateningHit = b;
    }

    public String getLastBiome() {
        return lastBiome;
    }

    public void setLastBiome(String b) {
        lastBiome = b;
    }

    public int getLookAtTimer() {
        return lookAtTimer;
    }

    public void setLookAtTimer(int v) {
        lookAtTimer = v;
    }

    public void incrementLookAtTimer() {
        lookAtTimer++;
    }

    public boolean isNocturnalCrouch() {
        return nocturnalCrouch;
    }

    public void randomizeNocturnalCrouch() {
        nocturnalCrouch = random.nextBoolean();
    }

    public int getModeTimer() {
        return modeTimer;
    }

    public void setModeTimer(int v) {
        modeTimer = v;
    }

    public boolean isMorphing() {
        return entityData.get(IS_MORPHING);
    }

    public void setMorphing(boolean b) {
        entityData.set(IS_MORPHING, b);
    }

    public boolean isScreaming() {
        return entityData.get(IS_SCREAMING);
    }

    public void setScreaming(boolean v) {
        entityData.set(IS_SCREAMING, v);
    }

    public boolean isEating() {
        return entityData.get(IS_EATING);
    }

    public void setEating(boolean b) {
        entityData.set(IS_EATING, b);
    }

    public boolean isMeleeHurt() {
        return entityData.get(IS_MELEE_HURT);
    }

    public void setMeleeHurt(boolean b) {
        entityData.set(IS_MELEE_HURT, b);
    }

    public boolean isRangedHurt() {
        return entityData.get(IS_RANGED_HURT);
    }

    public void setRangedHurt(boolean b) {
        entityData.set(IS_RANGED_HURT, b);
    }

    public boolean isWeakAttacking() {
        return entityData.get(IS_WEAK_ATTACKING);
    }

    public void setWeakAttacking(boolean b) {
        entityData.set(IS_WEAK_ATTACKING, b);
    }

    public boolean isStrongAttacking() {
        return entityData.get(IS_STRONG_ATTACKING);
    }

    public void setStrongAttacking(boolean b) {
        entityData.set(IS_STRONG_ATTACKING, b);
    }

    public boolean isDraggingPlayer() {
        return entityData.get(IS_DRAGGING);
    }

    public void setDraggingPlayer(boolean b) {
        entityData.set(IS_DRAGGING, b);
    }

    public boolean isCrouchingAnim() {
        return entityData.get(IS_CROUCHING_ANIM);
    }

    public void setCrouchingAnim(boolean b) {
        entityData.set(IS_CROUCHING_ANIM, b);
    }

    public boolean isLookingAround() {
        return entityData.get(IS_LOOKING_AROUND);
    }

    public void setLookingAround(boolean b) {
        entityData.set(IS_LOOKING_AROUND, b);
    }

    public boolean isMorphed() {
        return entityData.get(IS_MORPHED);
    }

    public void setMorphed(boolean v) {
        entityData.set(IS_MORPHED, v);
    }

    public String getMorphedInto() {
        return entityData.get(MORPHED_INTO);
    }

    public void setMorphedInto(String v) {
        entityData.set(MORPHED_INTO, v);
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void checkDespawn() {
        // Nothing
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        r.add(new AnimationController<>(this, "main", 5, s -> {
            if (isMorphing()) return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.morph"));
            if (isScreaming()) {
                if (s.getController().getAnimationState() == AnimationController.State.STOPPED) setScreaming(false);
                return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.scream"));
            }
            if (isMeleeHurt()) {
                if (s.getController().getAnimationState() == AnimationController.State.STOPPED) setMeleeHurt(false);
                return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.melee_hurt"));
            }
            if (isRangedHurt()) {
                if (s.getController().getAnimationState() == AnimationController.State.STOPPED) setRangedHurt(false);
                return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.ranged_hurt"));
            }
            if (isWeakAttacking()) {
                if (s.getController().getAnimationState() == AnimationController.State.STOPPED) setWeakAttacking(false);
                return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.weak_attack"));
            }
            if (isStrongAttacking()) {
                if (s.getController().getAnimationState() == AnimationController.State.STOPPED)
                    setStrongAttacking(false);
                return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.strong_attack"));
            }
            if (isDraggingPlayer())
                return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.catch_to_drag").thenLoop("animation.skinwalker.drag"));
            if (isEating()) {
                if (s.getController().getAnimationState() == AnimationController.State.STOPPED) setEating(false);
                return s.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.eat"));
            }
            if (isCrouchingAnim())
                return s.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.crouch"));
            if (isLookingAround())
                return s.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.look_around"));
            SkinwalkerMode m = getMode();
            double sp = getDeltaMovement().horizontalDistanceSqr();
            if (m == SkinwalkerMode.AGGRESSIVE && sp > .04)
                return s.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.sprint_enraged"));
            if (m == SkinwalkerMode.AGGRESSIVE && sp > .001)
                return s.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.walk_enraged"));
            if (sp > .04) return s.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.sprint"));
            if (sp > .001) return s.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.walk"));
            return s.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}