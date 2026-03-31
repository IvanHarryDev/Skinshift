package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.DeerJumpGoal;
import com.harry.wildcraft.init.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class DeerMaleEntity extends Animal implements GeoEntity {

    protected final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean hasCounterAttacked = false;
    private int     counterTimer       = 0;
    private static final int COUNTER_DURATION = 600;
    private int deathTimer = 0;
    private static final int DEATH_DELAY = 40;

    public DeerMaleEntity(EntityType<? extends DeerMaleEntity> type, Level level) {
        super(type, level);
        this.getNavigation().setCanFloat(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE,  8.0)
                .add(Attributes.FOLLOW_RANGE,  30.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DeerJumpGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class,
                e -> !((Player)e).isCreative() && !((Player)e).isSpectator(),
                10.0f, 1.2, 1.4, e -> true));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() {
                return counterTimer > 0 && !hasCounterAttacked && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return counterTimer > 0 && !hasCounterAttacked && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(4, new EatBlockGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!isAlive()) {
            deathTimer++;
            if (deathTimer < DEATH_DELAY) setPersistenceRequired();
            return;
        }
        if (counterTimer > 0) {
            counterTimer--;
            if (counterTimer == 0) {
                hasCounterAttacked = false;
                setTarget(null);
                setAggressive(false);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) {
            triggerAnim("events", "hurt");
            LivingEntity att = src.getEntity() instanceof LivingEntity le ? le : null;
            boolean valid = att != null
                    && !(att instanceof Player p && (p.isCreative() || p.isSpectator()));
            if (valid) {
                counterTimer       = COUNTER_DURATION;
                hasCounterAttacked = false;
                setTarget(att);
                setAggressive(true);
            }
        }
        return h;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("events", "attack");
            hasCounterAttacked = true;
            setTarget(null);
            setAggressive(false);
        }
        return hit;
    }

    @Override
    public void die(DamageSource src) { super.die(src); }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int loot, boolean recent) {
        spawnAtLocation(new ItemStack(Items.MUTTON, 2 + random.nextInt(2)));
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable net.minecraft.nbt.CompoundTag tag) {
        spawnData = super.finalizeSpawn(level, diff, spawnType, spawnData, tag);
        if (spawnType != MobSpawnType.SPAWNER && spawnType != MobSpawnType.SPAWN_EGG
                && spawnType != MobSpawnType.CHUNK_GENERATION
                && spawnType != MobSpawnType.STRUCTURE
                && level instanceof ServerLevel sl) {
            int groupSize = 1 + random.nextInt(4);
            for (int i = 1; i < groupSize; i++) {
                EntityType<?> type = random.nextBoolean()
                        ? com.harry.wildcraft.init.ModEntities.DEER_MALE.get()
                        : com.harry.wildcraft.init.ModEntities.DEER_FEMALE.get();
                Mob companion = (Mob) type.create(sl);
                if (companion == null) continue;
                double nx = getX() + (random.nextDouble() - 0.5) * 6;
                double nz = getZ() + (random.nextDouble() - 0.5) * 6;
                if (sl.hasChunk((int)nx >> 4, (int)nz >> 4)) {
                    companion.moveTo(nx, getY(), nz, random.nextFloat() * 360, 0);
                    sl.addFreshEntity(companion);
                }
            }
        }
        return spawnData;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.DEER_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return ModSounds.DEER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.25f;
    }
    @Override
    public int getAmbientSoundInterval() {
        return 700;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 3, state -> {
            if (!isAlive()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.deer.death"));
            }

            if (state.isMoving()) {
                double speedSq = getDeltaMovement().horizontalDistanceSqr();

                if (!onGround() && speedSq > 0.04) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.deer.sprint_jump"));
                }

                if (speedSq > 0.015 || isAggressive()) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.deer.sprint"));
                }

                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.deer.walk"));
            }

            if (tickCount % 600 < 60) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.deer.eat"));
            }

            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.deer.idle"));
        }));

        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.deer.attack"))
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay("animation.deer.hurt")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}