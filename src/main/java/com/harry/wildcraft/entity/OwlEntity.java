package com.harry.wildcraft.entity;

import com.harry.wildcraft.entity.goal.OwlFlyToTreeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

    public static final EntityDataAccessor<Boolean> IS_FLYING =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_WALKING =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_HURT_ANIM =
            SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);

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

    private static final double LANDED_THRESHOLD = 0.05;

    public OwlEntity(EntityType<? extends OwlEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_FLYING,    false);
        entityData.define(IS_WALKING,   false);
        entityData.define(IS_HURT_ANIM, false);
    }

    public boolean isFlying()   { return entityData.get(IS_FLYING); }
    public boolean isWalking()  { return entityData.get(IS_WALKING); }
    public boolean isHurtAnim() { return entityData.get(IS_HURT_ANIM); }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,    10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE,  16.0);
    }

    private double getDistanceToGround() {
        double feetY = getY();
        int blockX = (int) Math.floor(getX());
        int blockZ = (int) Math.floor(getZ());
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
            private BlockPos safeTreePos = null;

            @Override
            public boolean canUse() {
                if (level().isClientSide) return false;
                nearPlayer = level().getNearestPlayer(OwlEntity.this, 6.0);

                if (nearPlayer == null || nearPlayer.isCreative() || nearPlayer.isSpectator()) return false;

                safeTreePos = findEmergencyTree();
                return safeTreePos != null;
            }

            @Override
            public boolean canContinueToUse() {
                return safeTreePos != null && !getNavigation().isDone() && nearPlayer != null && distanceTo(nearPlayer) < 15.0;
            }

            @Override
            public void start() {
                if (safeTreePos != null) {
                    getNavigation().moveTo(safeTreePos.getX() + 0.5, safeTreePos.getY() + 1.0, safeTreePos.getZ() + 0.5, 1.5);
                }
            }

            private BlockPos findEmergencyTree() {
                BlockPos owlPos = blockPosition();
                for (int attempt = 0; attempt < 15; attempt++) {
                    int dx = random.nextInt(30) - 15;
                    int dy = random.nextInt(10) + 2;
                    int dz = random.nextInt(30) - 15;
                    BlockPos candidate = owlPos.offset(dx, dy, dz);

                    if (level().getBlockState(candidate).is(net.minecraft.tags.BlockTags.LEAVES)) {
                        return candidate;
                    }
                }
                return null;
            }
        });
        goalSelector.addGoal(1, new OwlFlyToTreeGoal(this));
        goalSelector.addGoal(2, new Goal() {
            private int moveTimer = 0;
            @Override public boolean canUse() {
                return walkDuration > 0 && distToGround <= LANDED_THRESHOLD;
            }
            @Override public boolean canContinueToUse() {
                return walkDuration > 0 && distToGround <= LANDED_THRESHOLD;
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

        distToGround = getDistanceToGround();
        boolean isLanded = distToGround <= LANDED_THRESHOLD;

        if (isInWater()) {
            Vec3 mov = getDeltaMovement();
            if (mov.y < 0.15)
                setDeltaMovement(mov.x, mov.y + 0.06, mov.z);
            isLanded = false;
        } else {
            if (!isLanded) {
                Vec3 mov = getDeltaMovement();
                if (mov.y > -0.08)
                    setDeltaMovement(mov.x, mov.y - 0.02, mov.z);
            } else {
                Vec3 mov = getDeltaMovement();
                if (mov.y < 0) setDeltaMovement(mov.x, 0, mov.z);
            }
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
        if (walkTimer >= WALK_INTERVAL && walkDuration == 0 && isLanded) {
            walkTimer    = 0;
            walkDuration = WALK_MAX;
        }

        entityData.set(IS_FLYING, !isLanded);
        boolean actuallyWalking = isLanded && walkDuration > 0
                && (Math.abs(getDeltaMovement().x) > 0.01 || Math.abs(getDeltaMovement().z) > 0.01);
        entityData.set(IS_WALKING,   actuallyWalking);
        entityData.set(IS_HURT_ANIM, hurtAnimTimer > 0);
    }

    @Override
    public boolean hurt(DamageSource src, float dmg) {
        boolean h = super.hurt(src, dmg);
        if (h) { hurtAnimTimer = HURT_ANIM_DURATION; triggerAnim("events", "hurt"); }
        return h;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int loot, boolean recent) {
        spawnAtLocation(new ItemStack(Items.FEATHER, 1 + random.nextInt(3)));
    }

    // ---- GeckoLib ----
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 2, state -> {
            if (!isAlive())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.idle"));
            if (isHurtAnim())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.idle"));
            if (isFlying())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.fly"));
            if (isWalking())
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.walk"));
            if (tickCount % 600 < 300)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.idle"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.owl.idle2"));
        }));
        registrar.add(new AnimationController<>(this, "events", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().thenPlay("animation.owl.hurt"))
                .triggerableAnim("hoot", RawAnimation.begin().thenPlay("animation.owl.idle")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}