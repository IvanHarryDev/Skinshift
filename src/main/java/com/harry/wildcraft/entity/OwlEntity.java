package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.OwlFlyToTreeGoal;
import com.harry.wildcraft.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class OwlEntity extends FlyingMob implements GeoEntity {

    public static final EntityDataAccessor<Integer> ANIM_STATE =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_HURT_ANIM =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int STATE_IDLE  = 0;
    public static final int STATE_IDLE2 = 1;
    public static final int STATE_FLY   = 2;
    public static final int STATE_WALK  = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int hootCooldown = 400 + (int)(Math.random() * 400);
    private static final int HOOT_INTERVAL      = 800;
    private int hurtAnimTimer = 0;
    private static final int HURT_ANIM_DURATION = 15;
    private int walkTimer    = 0;
    private int walkDuration = 0;
    private static final int WALK_INTERVAL = 300;
    private static final int WALK_MAX      = 60;

    private double distToGround = 999.0;
    private static final double LANDED_THRESHOLD = 0.15;

    private int clientPrevAnimState = -1;

    private int prevAnimState = STATE_IDLE;

    private static final RawAnimation ANIM_FLY   = RawAnimation.begin().thenLoop("animation.owl.fly");
    private static final RawAnimation ANIM_WALK  = RawAnimation.begin().thenLoop("animation.owl.walk");
    private static final RawAnimation ANIM_IDLE  = RawAnimation.begin().thenLoop("animation.owl.idle");
    private static final RawAnimation ANIM_IDLE2 = RawAnimation.begin().thenLoop("animation.owl.idle2");

    public OwlEntity(EntityType<? extends OwlEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM_STATE,   STATE_IDLE);
        entityData.define(IS_HURT_ANIM, false);
    }

    public int     getAnimState()    { return entityData.get(ANIM_STATE); }
    public boolean isFlying()        { return getAnimState() == STATE_FLY; }
    public boolean isWalking()       { return getAnimState() == STATE_WALK; }
    public boolean isHurtAnim()      { return entityData.get(IS_HURT_ANIM); }
    public double  getDistToGround() { return distToGround; }
    public boolean isLanded()        { return distToGround <= LANDED_THRESHOLD; }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE,  16.0);
    }

    private double computeDistanceToGround() {
        double halfWidth = getBbWidth() / 2.0 * 0.8;
        double feetY = getY();
        double cx = getX();
        double cz = getZ();

        double[] xOffsets = { 0, -halfWidth, halfWidth, -halfWidth, halfWidth };
        double[] zOffsets = { 0, -halfWidth, -halfWidth, halfWidth, halfWidth };

        double minDist = 999.0;
        for (int i = 0; i < xOffsets.length; i++) {
            double dist = getDistAtPoint(cx + xOffsets[i], feetY, cz + zOffsets[i]);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private double getDistAtPoint(double px, double feetY, double pz) {
        int blockX = (int) Math.floor(px);
        int blockZ = (int) Math.floor(pz);
        int startBlockY = (int) Math.floor(feetY) + 1;

        for (int dy = 0; dy <= 10; dy++) {
            BlockPos check = new BlockPos(blockX, startBlockY - dy, blockZ);
            BlockState bs = level().getBlockState(check);
            if (bs.isAir()) continue;
            if (!bs.isSolid()) continue;

            double shapeMaxY;
            try {
                VoxelShape shape = bs.getShape(level(), check);
                if (shape.isEmpty()) continue;
                shapeMaxY = shape.max(Direction.Axis.Y);
                if (Double.isInfinite(shapeMaxY) || shapeMaxY <= 0.0) continue;
            } catch (Exception e) {
                shapeMaxY = 1.0;
            }

            double blockTop = check.getY() + shapeMaxY;
            if (blockTop > feetY + 0.5) continue;

            return feetY - blockTop;
        }
        return 999.0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new Goal() {
            private Player nearPlayer = null;

            @Override
            public boolean canUse() {
                if (level().isClientSide) return false;
                nearPlayer = level().getNearestPlayer(OwlEntity.this, 6.0);
                if (nearPlayer == null) return false;
                return !nearPlayer.isCreative()
                        && !nearPlayer.isSpectator()
                        && !nearPlayer.isShiftKeyDown();
            }

            @Override
            public boolean canContinueToUse() {
                return nearPlayer != null && nearPlayer.isAlive()
                        && !nearPlayer.isCreative()
                        && !nearPlayer.isSpectator()
                        && !nearPlayer.isShiftKeyDown()
                        && distanceTo(nearPlayer) < 15.0;
            }

            @Override
            public void start() {
                applyFleeImpulse();
            }

            @Override
            public void tick() {
                if (nearPlayer != null && distanceTo(nearPlayer) < 8.0) {
                    applyFleeImpulse();
                }
            }

            private void applyFleeImpulse() {
                if (nearPlayer == null) return;
                double dx = getX() - nearPlayer.getX();
                double dz = getZ() - nearPlayer.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.01) {
                    dx = random.nextDouble() - 0.5;
                    dz = random.nextDouble() - 0.5;
                    len = Math.sqrt(dx * dx + dz * dz);
                }
                if (len > 0) {
                    setDeltaMovement(dx / len * 0.35, 0.25, dz / len * 0.35);
                }
            }
        });

        goalSelector.addGoal(1, new OwlFlyToTreeGoal(this));

        goalSelector.addGoal(2, new Goal() {
            private int moveTimer = 0;
            @Override public boolean canUse() {
                return walkDuration > 0 && isLanded();
            }
            @Override public boolean canContinueToUse() {
                return walkDuration > 0 && isLanded();
            }
            @Override public void start() { moveTimer = 0; }
            @Override public void tick() {
                moveTimer++;
                if (moveTimer % 20 == 0) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    setDeltaMovement(Math.cos(angle) * 0.12, getDeltaMovement().y,
                            Math.sin(angle) * 0.12);
                }
            }
        });

        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0f));
    }

    @Override
    public void tick() {
        super.tick();
        if (hurtAnimTimer > 0) hurtAnimTimer--;

        distToGround = computeDistanceToGround();
        boolean landed = isLanded();

        if (isInWater()) {
            Vec3 mov = getDeltaMovement();
            if (mov.y < 0.15)
                setDeltaMovement(mov.x, mov.y + 0.06, mov.z);
            landed = false;
        } else if (!landed) {
            Vec3 mov = getDeltaMovement();
            double gravity = -0.015;
            double dragH = 0.98;
            double newY = mov.y + gravity;
            if (newY < -0.06) newY = -0.06;
            setDeltaMovement(mov.x * dragH, newY, mov.z * dragH);
        } else {
            Vec3 mov = getDeltaMovement();
            if (mov.y < 0) setDeltaMovement(mov.x, 0, mov.z);
        }

        if (level().isClientSide) return;
        if (!isAlive()) return;

        hootCooldown--;
        if (hootCooldown <= 0) {
            triggerAnim("events", "hoot");
            hootCooldown = HOOT_INTERVAL + random.nextInt(400);
        }

        if (walkDuration > 0) walkDuration--;
        walkTimer++;
        if (walkTimer >= WALK_INTERVAL && walkDuration == 0 && landed) {
            walkTimer    = 0;
            walkDuration = WALK_MAX;
        }

        int newState;
        if (!landed) {
            newState = STATE_FLY;
        } else {
            boolean movingH = Math.abs(getDeltaMovement().x) > 0.01
                    || Math.abs(getDeltaMovement().z) > 0.01;
            if (walkDuration > 0 && movingH) {
                newState = STATE_WALK;
            } else {
                newState = (tickCount % 600 < 300) ? STATE_IDLE : STATE_IDLE2;
            }
        }
        if (newState == STATE_FLY && prevAnimState != STATE_FLY) {
            playSound(ModSounds.OWL_FLY.get(), 0.4f, 1.0f);
        }
        prevAnimState = newState;
        entityData.set(ANIM_STATE, newState);
        entityData.set(IS_HURT_ANIM, hurtAnimTimer > 0);
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) {
            hurtAnimTimer = HURT_ANIM_DURATION;
            triggerAnim("events", "hurt");
        }
        return h;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int loot, boolean recent) {
        spawnAtLocation(new ItemStack(Items.FEATHER, 1 + random.nextInt(3)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.OWL_IDLE.get();
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return ModSounds.OWL_HURT.get();
    }
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.OWL_DEATH.get();
    }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        AnimationController<OwlEntity> mainCtrl = new AnimationController<>(this, "main", 5, state -> {
            int current = getAnimState();

            if (!isAlive()) {
                if (clientPrevAnimState != -99) {
                    clientPrevAnimState = -99;
                    state.getController().setAnimation(ANIM_IDLE);
                }
                return PlayState.CONTINUE;
            }

            if (isHurtAnim()) {
                return PlayState.CONTINUE;
            }

            if (current != clientPrevAnimState) {
                clientPrevAnimState = current;
                switch (current) {
                    case STATE_FLY:
                        state.getController().setAnimation(ANIM_FLY);
                        break;
                    case STATE_WALK:
                        state.getController().setAnimation(ANIM_WALK);
                        break;
                    case STATE_IDLE2:
                        state.getController().setAnimation(ANIM_IDLE2);
                        break;
                    default:
                        state.getController().setAnimation(ANIM_IDLE);
                        break;
                }
            }

            return PlayState.CONTINUE;
        });

        mainCtrl.setAnimation(ANIM_IDLE);

        registrar.add(mainCtrl);

        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay("animation.owl.hurt"))
                .triggerableAnim("hoot", RawAnimation.begin().thenPlay("animation.owl.idle")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}