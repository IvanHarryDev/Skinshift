package com.harry.wildcraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

public class CoyoteEntity extends Animal implements GeoEntity {

    public enum PackState { IDLE, STALKING, ATTACKING }
    private PackState packState = PackState.IDLE;

    public static final EntityDataAccessor<Boolean> IS_CHASING =
            SynchedEntityData.defineId(CoyoteEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_HOWLING =
            SynchedEntityData.defineId(CoyoteEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_STALKING_ANIM =
            SynchedEntityData.defineId(CoyoteEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean hasHowledForCombat = false;
    private int howlAnimTimer = 0;
    private static final int HOWL_ANIM_DURATION = 100;

    private int combatTimer = 0;
    private static final int COMBAT_TIMEOUT = 200;

    private int deathTimer = 0;
    private static final int DEATH_DELAY = 20;

    private int fleeFromGazeTimer = 0;
    private static final int FLEE_GAZE_DURATION = 40;

    public CoyoteEntity(EntityType<? extends CoyoteEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_CHASING,       false);
        entityData.define(IS_HOWLING,       false);
        entityData.define(IS_STALKING_ANIM, false);
    }

    public boolean isChasing()      { return entityData.get(IS_CHASING); }
    public boolean isHowling()      { return entityData.get(IS_HOWLING); }
    public boolean isStalkingAnim() { return entityData.get(IS_STALKING_ANIM); }

    public void setPackState(PackState s) { setPackStateAll(s); }
    public PackState getPackState()       { return packState; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE,  4.0)
                .add(Attributes.FOLLOW_RANGE,  150.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));

        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() {
                return packState == PackState.ATTACKING && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return packState == PackState.ATTACKING && super.canContinueToUse();
            }
        });

        goalSelector.addGoal(2, new Goal() {
            private Player stalkTarget = null;
            private int repositionTimer = 0;
            @Override public boolean canUse() {
                if (packState == PackState.ATTACKING) return false;
                if (level().isClientSide) return false;
                stalkTarget = level().getNearestPlayer(CoyoteEntity.this, 150.0);
                if (stalkTarget == null) return false;
                return !stalkTarget.isCreative() && !stalkTarget.isSpectator();
            }
            @Override public boolean canContinueToUse() {
                if (packState == PackState.ATTACKING) return false;
                return stalkTarget != null && stalkTarget.isAlive()
                        && !stalkTarget.isCreative() && !stalkTarget.isSpectator()
                        && distanceTo(stalkTarget) <= 150.0;
            }
            @Override public void start() {
                packState = PackState.STALKING;
                repositionTimer = 0;
                fleeFromGazeTimer = 0;
                if (!hasHowledForCombat) { fireHowl(); hasHowledForCombat = true; }
            }
            @Override public void stop() {
                if (packState == PackState.STALKING) packState = PackState.IDLE;
                stalkTarget = null;
                getNavigation().stop();
            }
            @Override public void tick() {
                if (stalkTarget == null) return;
                double dist = distanceTo(stalkTarget);
                if (dist < 5.0) { setPackStateAll(PackState.ATTACKING); setTarget(stalkTarget); return; }
                if (dist > 30.0 && fleeFromGazeTimer == 0 && isPlayerLookingAtMe(stalkTarget))
                    fleeFromGazeTimer = FLEE_GAZE_DURATION;
                if (fleeFromGazeTimer > 0) {
                    fleeFromGazeTimer--;
                    Vec3 away = position().subtract(stalkTarget.position()).normalize();
                    getNavigation().moveTo(getX() + away.x * 8, getY(), getZ() + away.z * 8, 1.5);
                    return;
                }
                repositionTimer++;
                if (repositionTimer % 30 == 0) {
                    double angleOffset = (getId() % 6) * (Math.PI * 2 / 6);
                    double angle  = angleOffset + (repositionTimer * 0.02);
                    double radius = Math.max(dist - 3.0, 15.0);
                    getNavigation().moveTo(
                            stalkTarget.getX() + Math.cos(angle) * radius,
                            stalkTarget.getY(),
                            stalkTarget.getZ() + Math.sin(angle) * radius, 0.75);
                }
                getLookControl().setLookAt(stalkTarget, 30, 30);
            }
        });

        goalSelector.addGoal(3, new Goal() {
            private CoyoteEntity packMate = null;
            @Override public boolean canUse() {
                if (packState != PackState.IDLE) return false;
                if (level().isClientSide) return false;
                List<CoyoteEntity> nearby = level().getEntitiesOfClass(
                        CoyoteEntity.class, getBoundingBox().inflate(60.0),
                        c -> c != CoyoteEntity.this && c.isAlive());
                if (nearby.isEmpty()) return false;
                packMate = nearby.stream()
                        .min((a, b) -> Double.compare(distanceTo(a), distanceTo(b))).orElse(null);
                return packMate != null && distanceTo(packMate) > 10.0;
            }
            @Override public boolean canContinueToUse() {
                return packMate != null && packMate.isAlive()
                        && distanceTo(packMate) > 5.0 && packState == PackState.IDLE;
            }
            @Override public void stop() { packMate = null; getNavigation().stop(); }
            @Override public void tick() { if (packMate != null) getNavigation().moveTo(packMate, 0.85); }
        });

        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() {
                if (packState == PackState.ATTACKING) return false;
                if (level().getNearestPlayer(CoyoteEntity.this, 150) != null) return false;
                return super.canUse();
            }
            @Override public boolean canContinueToUse() {
                if (packState == PackState.ATTACKING) return false;
                if (level().getNearestPlayer(CoyoteEntity.this, 150) != null) return false;
                return super.canContinueToUse();
            }
        });

        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
            @Override public boolean canUse() {
                return packState == PackState.IDLE && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));

        targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers(CoyoteEntity.class));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false,
                e -> packState == PackState.ATTACKING
                        && !((Player)e).isCreative() && !((Player)e).isSpectator()));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, false,
                e -> e instanceof Animal && !(e instanceof CoyoteEntity)
                        && level().getNearestPlayer(this, 150) == null));
    }

    private boolean isPlayerLookingAtMe(Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toUs = position().subtract(player.position()).normalize();
        return look.dot(toUs) > 0.95;
    }

    private void setPackStateAll(PackState newState) {
        PackState old = this.packState;
        this.packState = newState;
        if (newState == PackState.IDLE && old != PackState.IDLE) {
            hasHowledForCombat = false;
            combatTimer = 0;
        }
        if (level() instanceof ServerLevel sl) {
            sl.getEntitiesOfClass(CoyoteEntity.class,
                            getBoundingBox().inflate(50), c -> c != this && c.isAlive())
                    .forEach(c -> {
                        PackState cOld = c.packState;
                        c.packState = newState;
                        if (newState == PackState.IDLE && cOld != PackState.IDLE) {
                            c.hasHowledForCombat = false;
                            c.combatTimer = 0;
                        }
                        if (newState == PackState.ATTACKING && getTarget() != null)
                            c.setTarget(getTarget());
                    });
        }
    }

    private void fireHowl() {
        if (howlAnimTimer > 0) return;
        triggerAnim("events", "howl");
        entityData.set(IS_HOWLING, true);
        howlAnimTimer = HOWL_ANIM_DURATION;
        getNavigation().stop();
        if (level() instanceof ServerLevel sl) {
            sl.getEntitiesOfClass(CoyoteEntity.class,
                            getBoundingBox().inflate(20), c -> c != this && c.isAlive())
                    .forEach(c -> {
                        if (c.howlAnimTimer == 0) {
                            c.triggerAnim("events", "howl");
                            c.entityData.set(IS_HOWLING, true);
                            c.howlAnimTimer = HOWL_ANIM_DURATION;
                            c.getNavigation().stop();
                        }
                    });
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (howlAnimTimer > 0) {
            howlAnimTimer--;
            getNavigation().stop();
            if (howlAnimTimer == 0 && !level().isClientSide)
                entityData.set(IS_HOWLING, false);
        }

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

        double speed = (packState == PackState.ATTACKING || combatTimer > 0) ? 0.32 : 0.22;
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null)
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);

        if (packState == PackState.ATTACKING
                && getTarget() instanceof Player p && distanceTo(p) > 150) {
            setPackStateAll(PackState.IDLE);
            setTarget(null);
        }

        boolean chasingOrFleeing = (hasTarget && (packState == PackState.ATTACKING || combatTimer > 0)) || fleeFromGazeTimer > 0;

        entityData.set(IS_CHASING, chasingOrFleeing);

        entityData.set(IS_STALKING_ANIM, packState == PackState.STALKING && !hasTarget && fleeFromGazeTimer == 0);
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) {
            combatTimer = COMBAT_TIMEOUT;
            if (src.getEntity() instanceof Player p
                    && !p.isCreative() && !p.isSpectator()) {
                if (!hasHowledForCombat) { fireHowl(); hasHowledForCombat = true; }
                setPackStateAll(PackState.ATTACKING);
                setTarget(p);
            }
            triggerAnim("events", "hurt");
        }
        return h;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            combatTimer = COMBAT_TIMEOUT;
            if (target instanceof Player) setPackStateAll(PackState.ATTACKING);
            triggerAnim("events", "attack");
        }
        return hit;
    }

    @Override
    public void die(DamageSource src) { super.die(src); }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable net.minecraft.nbt.CompoundTag tag) {
        spawnData = super.finalizeSpawn(level, diff, spawnType, spawnData, tag);
        if (spawnType != MobSpawnType.SPAWNER
                && spawnType != MobSpawnType.CHUNK_GENERATION
                && spawnType != MobSpawnType.STRUCTURE
                && level instanceof ServerLevel sl) {
            int groupSize = 2 + random.nextInt(4);
            for (int i = 0; i < groupSize; i++) {
                CoyoteEntity companion = new CoyoteEntity(
                        com.harry.wildcraft.init.ModEntities.COYOTE.get(), sl);
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

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (!isAlive())
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.death"));

            boolean moving = state.isMoving();

            if (isHowling()) {
                state.getController().setAnimationSpeed(1.0);
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.idle"));
            }

            if (isChasing() && moving) {
                state.getController().setAnimationSpeed(1.0);
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.sprint"));
            }

            if (isStalkingAnim() && moving) {
                state.getController().setAnimationSpeed(1.5);
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.stalk"));
            }

            if (moving) {
                state.getController().setAnimationSpeed(1.4);
                return state.setAndContinue(
                        RawAnimation.begin().thenLoop("animation.coyote.walk"));
            }

            state.getController().setAnimationSpeed(1.0);
            return state.setAndContinue(
                    RawAnimation.begin().thenLoop("animation.coyote.idle"));
        }));

        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("attack",
                        RawAnimation.begin().thenPlay("animation.coyote.attack"))
                .triggerableAnim("hurt",
                        RawAnimation.begin().thenPlay("animation.coyote.hurt"))
                .triggerableAnim("howl",
                        RawAnimation.begin().thenPlay("animation.coyote.howl")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}