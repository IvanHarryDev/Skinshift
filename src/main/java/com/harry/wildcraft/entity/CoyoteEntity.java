package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.CoyotePackAttackGoal;
import com.harry.wildcraft.entity.goal.CoyoteStalkGoal;
import com.harry.wildcraft.init.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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

    public enum PackState { IDLE, STALKING, HOWLING, ATTACKING }
    private PackState packState = PackState.IDLE;

    public static final EntityDataAccessor<Integer> ANIM_STATE =
            SynchedEntityData.defineId(CoyoteEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_HOWLING =
            SynchedEntityData.defineId(CoyoteEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int ANIM_IDLE    = 0;
    public static final int ANIM_WALK    = 1;
    public static final int ANIM_STALK   = 2;
    public static final int ANIM_SPRINT  = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean hasHowledForCombat = false;
    private int howlAnimTimer = 0;
    public static final int HOWL_ANIM_DURATION = 80;

    private int combatTimer = 0;
    public static final int COMBAT_TIMEOUT = 200;

    private int deathTimer = 0;
    private static final int DEATH_DELAY = 20;

    public int fleeFromGazeTimer = 0;
    public static final int FLEE_GAZE_DURATION = 40;

    private Player pendingAttackTarget = null;

    public static final double STALK_DETECT_RANGE = 40.0;
    public static final double ATTACK_TRIGGER_RANGE = 5.0;
    public static final double PACK_COORDINATION_RANGE = 50.0;
    public static final double MAX_CHASE_RANGE = 60.0;

    public static final double PACK_LEASH_RANGE = 15.0;
    public static final double PACK_MERGE_RANGE = 40.0;

    private static final RawAnimation RANIM_IDLE   = RawAnimation.begin().thenLoop("animation.coyote.idle");
    private static final RawAnimation RANIM_WALK   = RawAnimation.begin().thenLoop("animation.coyote.walk");
    private static final RawAnimation RANIM_SPRINT = RawAnimation.begin().thenLoop("animation.coyote.sprint");
    private static final RawAnimation RANIM_STALK  = RawAnimation.begin().thenLoop("animation.coyote.stalk");
    private static final RawAnimation RANIM_DEATH  = RawAnimation.begin().thenLoop("animation.coyote.death");
    private static final RawAnimation RANIM_HOWL   = RawAnimation.begin().thenPlay("animation.coyote.howl");
    private static final RawAnimation RANIM_ATTACK = RawAnimation.begin().thenPlay("animation.coyote.attack");
    private static final RawAnimation RANIM_HURT   = RawAnimation.begin().thenPlay("animation.coyote.hurt");

    public CoyoteEntity(EntityType<? extends CoyoteEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM_STATE, ANIM_IDLE);
        entityData.define(IS_HOWLING, false);
    }

    public int getAnimStateSynced()  { return entityData.get(ANIM_STATE); }
    public boolean isHowling()       { return entityData.get(IS_HOWLING); }
    public PackState getPackState()  { return packState; }
    public void setPackStateDirect(PackState s) { this.packState = s; }
    public boolean hasHowledForCombat() { return hasHowledForCombat; }
    public void setHasHowledForCombat(boolean v) { hasHowledForCombat = v; }
    public int getHowlAnimTimer()    { return howlAnimTimer; }
    public int getCombatTimer()      { return combatTimer; }
    public void setCombatTimer(int v) { combatTimer = v; }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE,  4.0)
                .add(Attributes.FOLLOW_RANGE,  40.0);
    }

    // GOALS
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CoyotePackAttackGoal(this));
        goalSelector.addGoal(2, new CoyoteStalkGoal(this));
        goalSelector.addGoal(3, new CoyotePackPatrolGoal(this));

        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() {
                if (packState != PackState.IDLE) return false;
                if (level().getNearestPlayer(CoyoteEntity.this, STALK_DETECT_RANGE) != null)
                    return false;
                return super.canUse();
            }
            @Override public boolean canContinueToUse() {
                if (packState != PackState.IDLE) return false;
                if (level().getNearestPlayer(CoyoteEntity.this, STALK_DETECT_RANGE) != null)
                    return false;
                return super.canContinueToUse();
            }
        });

        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));

        targetSelector.addGoal(0, new HurtByTargetGoal(this).setAlertOthers(CoyoteEntity.class));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false,
                e -> packState == PackState.ATTACKING
                        && !((Player)e).isCreative() && !((Player)e).isSpectator()));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, false,
                e -> e instanceof Animal && !(e instanceof CoyoteEntity)
                        && level().getNearestPlayer(this, STALK_DETECT_RANGE) == null));
    }

    // PACK PATROL GOAL
    private static class CoyotePackPatrolGoal extends Goal {
        private final CoyoteEntity coyote;
        private int patrolTimer = 0;
        private int mergeCheckTimer = 0;

        public CoyotePackPatrolGoal(CoyoteEntity coyote) {
            this.coyote = coyote;
        }

        @Override
        public boolean canUse() {
            return coyote.packState == PackState.IDLE && !coyote.level().isClientSide;
        }

        @Override
        public boolean canContinueToUse() {
            return coyote.packState == PackState.IDLE;
        }

        @Override
        public void tick() {
            patrolTimer++;
            mergeCheckTimer++;

            CoyoteEntity nearest = findNearestPackmate();

            if (nearest != null) {
                double distToNearest = coyote.distanceTo(nearest);

                if (distToNearest > PACK_LEASH_RANGE) {
                    coyote.getNavigation().moveTo(nearest, 1.0);
                    return;
                }
            }

            if (mergeCheckTimer % 100 == 0) {
                tryMergeWithNearbyPack();
            }

            if (patrolTimer % 60 == 0) {
                Vec3 packCenter = getPackCenter();
                if (packCenter != null) {
                    double angle = coyote.getRandom().nextDouble() * Math.PI * 2;
                    double radius = 3.0 + coyote.getRandom().nextDouble() * 8.0;
                    double tx = packCenter.x + Math.cos(angle) * radius;
                    double tz = packCenter.z + Math.sin(angle) * radius;
                    coyote.getNavigation().moveTo(tx, packCenter.y, tz, 0.8);
                } else {
                    double angle = coyote.getRandom().nextDouble() * Math.PI * 2;
                    double dist = 5.0 + coyote.getRandom().nextDouble() * 10.0;
                    coyote.getNavigation().moveTo(
                            coyote.getX() + Math.cos(angle) * dist,
                            coyote.getY(),
                            coyote.getZ() + Math.sin(angle) * dist, 0.7);
                }
            }
        }

        private CoyoteEntity findNearestPackmate() {
            List<CoyoteEntity> pack = coyote.level().getEntitiesOfClass(
                    CoyoteEntity.class,
                    coyote.getBoundingBox().inflate(PACK_COORDINATION_RANGE),
                    c -> c != coyote && c.isAlive());
            if (pack.isEmpty()) return null;
            return pack.stream()
                    .min((a, b) -> Double.compare(coyote.distanceTo(a), coyote.distanceTo(b)))
                    .orElse(null);
        }

        private Vec3 getPackCenter() {
            List<CoyoteEntity> pack = coyote.level().getEntitiesOfClass(
                    CoyoteEntity.class,
                    coyote.getBoundingBox().inflate(PACK_LEASH_RANGE + 5),
                    c -> c != coyote && c.isAlive());
            if (pack.isEmpty()) return null;

            double sumX = coyote.getX(), sumY = coyote.getY(), sumZ = coyote.getZ();
            for (CoyoteEntity c : pack) {
                sumX += c.getX();
                sumY += c.getY();
                sumZ += c.getZ();
            }
            int count = pack.size() + 1;
            return new Vec3(sumX / count, sumY / count, sumZ / count);
        }

        private void tryMergeWithNearbyPack() {
            List<CoyoteEntity> distant = coyote.level().getEntitiesOfClass(
                    CoyoteEntity.class,
                    coyote.getBoundingBox().inflate(PACK_MERGE_RANGE),
                    c -> c != coyote && c.isAlive()
                            && coyote.distanceTo(c) > PACK_LEASH_RANGE);

            if (!distant.isEmpty()) {
                CoyoteEntity mergeTo = distant.stream()
                        .min((a, b) -> Double.compare(coyote.distanceTo(a), coyote.distanceTo(b)))
                        .orElse(null);
                if (mergeTo != null) {
                    coyote.getNavigation().moveTo(mergeTo, 0.9);
                }
            }
        }
    }

    // PACK COORDINATION
    public void alertPackToStalk(Player target) {
        if (!(level() instanceof ServerLevel sl)) return;
        sl.getEntitiesOfClass(CoyoteEntity.class,
                        getBoundingBox().inflate(PACK_COORDINATION_RANGE),
                        c -> c != this && c.isAlive())
                .forEach(c -> {
                    if (c.packState == PackState.IDLE) {
                        c.packState = PackState.STALKING;
                        c.hasHowledForCombat = true;
                        c.combatTimer = 0;
                    }
                });
    }

    public void fireHowlThenAttack(Player target) {
        if (howlAnimTimer > 0) return;

        packState = PackState.HOWLING;
        pendingAttackTarget = target;
        triggerAnim("events", "howl");
        entityData.set(IS_HOWLING, true);
        howlAnimTimer = HOWL_ANIM_DURATION;
        getNavigation().stop();

        if (level() instanceof ServerLevel sl) {
            sl.getEntitiesOfClass(CoyoteEntity.class,
                            getBoundingBox().inflate(30),
                            c -> c != this && c.isAlive())
                    .forEach(c -> {
                        if (c.howlAnimTimer == 0) {
                            c.packState = PackState.HOWLING;
                            c.pendingAttackTarget = target;
                            c.triggerAnim("events", "howl");
                            c.entityData.set(IS_HOWLING, true);
                            c.howlAnimTimer = HOWL_ANIM_DURATION;
                            c.getNavigation().stop();
                        }
                    });
        }
    }

    public void setPackStateAll(PackState newState) {
        PackState old = this.packState;
        this.packState = newState;

        if (newState == PackState.IDLE && old != PackState.IDLE) {
            hasHowledForCombat = false;
            combatTimer = 0;
        }
        if (newState == PackState.STALKING) {
            combatTimer = 0;
        }

        if (level() instanceof ServerLevel sl) {
            sl.getEntitiesOfClass(CoyoteEntity.class,
                            getBoundingBox().inflate(PACK_COORDINATION_RANGE),
                            c -> c != this && c.isAlive())
                    .forEach(c -> {
                        PackState cOld = c.packState;
                        c.packState = newState;
                        if (newState == PackState.IDLE && cOld != PackState.IDLE) {
                            c.hasHowledForCombat = false;
                            c.combatTimer = 0;
                        }
                        if (newState == PackState.STALKING) {
                            c.combatTimer = 0;
                        }
                        if (newState == PackState.ATTACKING && getTarget() != null) {
                            c.setTarget(getTarget());
                            c.combatTimer = COMBAT_TIMEOUT;
                        }
                    });
        }
    }

    public boolean isPlayerLookingAtMe(Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toUs = position().subtract(player.position()).normalize();
        return look.dot(toUs) > 0.92;
    }

    // TICK
    @Override
    public void tick() {
        super.tick();

        if (howlAnimTimer > 0) {
            howlAnimTimer--;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);

            if (howlAnimTimer == 0 && !level().isClientSide) {
                entityData.set(IS_HOWLING, false);

                if (pendingAttackTarget != null && pendingAttackTarget.isAlive()) {
                    packState = PackState.ATTACKING;
                    setTarget(pendingAttackTarget);
                    combatTimer = COMBAT_TIMEOUT;

                    if (level() instanceof ServerLevel sl) {
                        sl.getEntitiesOfClass(CoyoteEntity.class,
                                        getBoundingBox().inflate(PACK_COORDINATION_RANGE),
                                        c -> c != this && c.isAlive())
                                .forEach(c -> {
                                    c.packState = PackState.ATTACKING;
                                    c.setTarget(pendingAttackTarget);
                                    c.combatTimer = COMBAT_TIMEOUT;
                                    c.pendingAttackTarget = null;
                                });
                    }
                } else {
                    packState = PackState.IDLE;
                }
                pendingAttackTarget = null;
            }
        }

        if (level().isClientSide) return;
        if (!isAlive()) {
            deathTimer++;
            if (deathTimer < DEATH_DELAY) setPersistenceRequired();
            return;
        }
        if (howlAnimTimer > 0) return;

        boolean hasTarget = getTarget() != null && getTarget().isAlive();

        if (hasTarget && packState == PackState.ATTACKING) {
            combatTimer = COMBAT_TIMEOUT;
        } else if (packState != PackState.ATTACKING) {
            combatTimer = 0;
        } else if (combatTimer > 0) {
            combatTimer--;
            if (combatTimer == 0 && !hasTarget) {
                setPackStateAll(PackState.IDLE);
            }
        }

        double speed;
        if (packState == PackState.ATTACKING) {
            speed = 0.32;
        } else if (packState == PackState.STALKING) {
            speed = 0.18;
        } else {
            speed = 0.22;
        }
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null)
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);

        if (packState == PackState.ATTACKING
                && getTarget() instanceof Player p && distanceTo(p) > MAX_CHASE_RANGE) {
            setPackStateAll(PackState.IDLE);
            setTarget(null);
        }

        // ANIMATION STATE SYNC
        int newAnimState;
        if (fleeFromGazeTimer > 0) {
            newAnimState = ANIM_SPRINT;
        } else if (packState == PackState.ATTACKING || (combatTimer > 0 && hasTarget)) {
            newAnimState = ANIM_SPRINT;
        } else if (packState == PackState.STALKING) {
            newAnimState = ANIM_STALK;
        } else {
            newAnimState = ANIM_WALK;
        }
        entityData.set(ANIM_STATE, newAnimState);
    }

    // COMBAT
    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) {
            combatTimer = COMBAT_TIMEOUT;
            if (src.getEntity() instanceof Player p
                    && !p.isCreative() && !p.isSpectator()) {
                if (!hasHowledForCombat) {
                    hasHowledForCombat = true;
                    fireHowlThenAttack(p);
                } else {
                    setPackStateAll(PackState.ATTACKING);
                    setTarget(p);
                }
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
            if (target instanceof Player)
                setPackStateAll(PackState.ATTACKING);
            triggerAnim("events", "attack");
        }
        return hit;
    }

    @Override
    public void die(DamageSource src) { super.die(src); }

    // SPAWNING
    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable net.minecraft.nbt.CompoundTag tag) {
        spawnData = super.finalizeSpawn(level, diff, spawnType, spawnData, tag);
        if (spawnType != MobSpawnType.SPAWNER
                && spawnType != MobSpawnType.SPAWN_EGG
                && spawnType != MobSpawnType.CHUNK_GENERATION
                && spawnType != MobSpawnType.STRUCTURE
                && level instanceof ServerLevel sl) {

            List<CoyoteEntity> existing = sl.getEntitiesOfClass(
                    CoyoteEntity.class, getBoundingBox().inflate(40),
                    c -> c != this && c.isAlive());
            if (existing.size() >= 4) {
                return spawnData;
            }

            int groupSize = 1 + random.nextInt(2);
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

    // SOUNDS
    @Override protected SoundEvent getAmbientSound() {
        if (packState == PackState.STALKING || packState == PackState.HOWLING) return null;
        return ModSounds.COYOTE_IDLE.get();
    }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.COYOTE_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.COYOTE_DEATH.get(); }
    @Override protected float getSoundVolume() { return 0.3f; }
    @Override public int getAmbientSoundInterval() { return 600; }

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // GECKOLIB
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (!isAlive()) return state.setAndContinue(RANIM_DEATH);

            if (isHowling()) {
                state.getController().setAnimationSpeed(1.0);
                return state.setAndContinue(RANIM_IDLE);
            }

            int animState = getAnimStateSynced();
            boolean moving = state.isMoving();

            if (animState == ANIM_SPRINT) {
                state.getController().setAnimationSpeed(1.0);
                if (moving) {
                    return state.setAndContinue(RANIM_SPRINT);
                }
                return state.setAndContinue(RANIM_SPRINT);
            }

            if (animState == ANIM_STALK) {
                state.getController().setAnimationSpeed(moving ? 1.5 : 0.5);
                return state.setAndContinue(RANIM_STALK);
            }

            if (moving) {
                state.getController().setAnimationSpeed(1.4);
                return state.setAndContinue(RANIM_WALK);
            }

            state.getController().setAnimationSpeed(1.0);
            return state.setAndContinue(RANIM_IDLE);
        }));

        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RANIM_ATTACK)
                .triggerableAnim("hurt", RANIM_HURT)
                .triggerableAnim("howl", RANIM_HOWL));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}