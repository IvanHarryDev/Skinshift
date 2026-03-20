package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerAggressiveGoal;
import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerBreakBlocksGoal;
import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerDragPlayerGoal;
import com.harry.wildcraft.entity.skinwalker.goal.SkinwalkerFollowPlayerGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
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

    private UUID targetPlayerUUID;
    private int  modeTimer        = 0;
    private boolean isDraggingPlayer  = false;
    private boolean modeLocked        = false;
    private boolean pendingThreateningHit = false;
    private String lastBiome          = "";
    int  lookAtTimer      = 0;

    public static final int PASSIVE_DURATION     = 20 * 60 * 60 * 3;
    public static final int THREATENING_DURATION = 20 * 60 * 60 * 2;

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
        entityData.define(MODE,        SkinwalkerMode.PASSIVE.id);
        entityData.define(MORPHED_INTO, "none");
        entityData.define(IS_MORPHED,   false);
        entityData.define(IS_SCREAMING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new SkinwalkerFollowPlayerGoal(this));
        goalSelector.addGoal(1, new SkinwalkerDragPlayerGoal(this));
        goalSelector.addGoal(2, new SkinwalkerAggressiveGoal(this));
        goalSelector.addGoal(3, new SkinwalkerBreakBlocksGoal(this));
    }

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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;

        SkinwalkerMode mode = getMode();
        boolean isProjectile = source.getDirectEntity() instanceof Projectile;

        if (mode == SkinwalkerMode.PASSIVE) {
            if (source.getEntity() instanceof Player p) {
                poofAndRespawn((ServerLevel) level(), p);
            }
            return false;
        }

        if (mode == SkinwalkerMode.THREATENING) {
            if (source.getEntity() instanceof net.minecraft.world.entity.player.Player p) {
                p.hurt(level().damageSources().mobAttack(this), 2.0f);
                poofAndRespawn((ServerLevel) level(), p);
            }
            return false;
        }

        if (entityData.get(IS_MORPHED) && !isProjectile) {
            entityData.set(IS_MORPHED,   false);
            entityData.set(MORPHED_INTO, "none");
        }
        if (isProjectile && isDraggingPlayer) {
            isDraggingPlayer = false;
            entityData.set(IS_SCREAMING, true);
            pendingThreateningHit = true;
            return false;
        }
        if (isProjectile) {
            entityData.set(IS_SCREAMING, true);
        }
        return super.hurt(source, amount);
    }

    public void poofAndRespawn(ServerLevel level, net.minecraft.world.entity.player.Player target) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                getX(), getY() + 1, getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        net.minecraft.world.phys.Vec3 newPos =
                com.harry.wildcraft.util.SightHelper
                        .findPositionOutOfSight(level, (net.minecraft.server.level.ServerPlayer) target, 100, 15);
        if (newPos != null) {
            teleportTo(newPos.x, newPos.y, newPos.z);
            SkinwalkerMorphHelper.morphToClosestBiomeAnimal(this, level, newPos);
            lookAtTimer = 0;
        }
    }

    // ---- Getters / Setters ----
    public SkinwalkerMode getMode()             { return SkinwalkerMode.fromId(entityData.get(MODE)); }
    public void           setMode(SkinwalkerMode m) { entityData.set(MODE, m.id); }
    public void           setModeLocked(boolean b)  { this.modeLocked = b; }
    public UUID           getTargetPlayerUUID()     { return targetPlayerUUID; }
    public void           setTargetPlayer(UUID uuid){ this.targetPlayerUUID = uuid; }
    public boolean        isDraggingPlayer()        { return isDraggingPlayer; }
    public void           setDraggingPlayer(boolean b){ this.isDraggingPlayer = b; }
    public boolean        isPendingThreateningHit() { return pendingThreateningHit; }
    public void           setPendingThreateningHit(boolean b){ this.pendingThreateningHit = b; }
    public String         getLastBiome()            { return lastBiome; }
    public void           setLastBiome(String b)    { this.lastBiome = b; }
    public int  getLookAtTimer()          { return lookAtTimer; }
    public void setLookAtTimer(int v)      { this.lookAtTimer = v; }
    public void incrementLookAtTimer()     { this.lookAtTimer++; }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar r) {
        r.add(new AnimationController<>(this, "main", 5, state -> {
            SkinwalkerMode mode = getMode();
            if (Boolean.TRUE.equals(entityData.get(IS_SCREAMING)))
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.skinwalker.scream"));
            if (isDraggingPlayer)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.drag"));
            if (mode == SkinwalkerMode.AGGRESSIVE && this.isAggressive())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.run"));
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.skinwalker.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public boolean isMorphed() {
        return entityData.get(IS_MORPHED);
    }
    public void setMorphed(boolean value) {
        entityData.set(IS_MORPHED, value);
    }
    public String getMorphedInto() {
        return entityData.get(MORPHED_INTO);
    }
    public void setMorphedInto(String value) {
        entityData.set(MORPHED_INTO, value);
    }
    public boolean isScreaming() {
        return entityData.get(IS_SCREAMING);
    }
    public void setScreaming(boolean value) {
        entityData.set(IS_SCREAMING, value);
    }
    public boolean isMorphedPublic() {
        return entityData.get(IS_MORPHED);
    }
}