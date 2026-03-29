package com.harry.wildcraft.block;

import com.harry.wildcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class TrapBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    private UUID capturedEntityUUID  = null;
    private LivingEntity capturedEntityCache = null;

    private boolean pendingSnapAnim = false;
    private boolean pendingOpenAnim = false;

    private int rearmCooldown = 0;
    private static final int REARM_COOLDOWN_TICKS = 200; // 10 seconds

    private static final RawAnimation ANIM_CLOSE =
            RawAnimation.begin().thenPlay("animation.bear_trap.close");
    private static final RawAnimation ANIM_OPEN =
            RawAnimation.begin().thenPlay("animation.bear_trap.open");

    public TrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAP_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isOnCooldown() {
        return rearmCooldown > 0;
    }

    public boolean consumeSnapAnim() {
        if (pendingSnapAnim) { pendingSnapAnim = false; return true; }
        return false;
    }
    public boolean consumeOpenAnim() {
        if (pendingOpenAnim) { pendingOpenAnim = false; return true; }
        return false;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "main", 0,
                        state -> PlayState.STOP)
                        .triggerableAnim("snap", ANIM_CLOSE)
                        .triggerableAnim("open", ANIM_OPEN)
        );
    }

    public void captureEntity(LivingEntity entity) {
        capturedEntityUUID  = entity.getUUID();
        capturedEntityCache = entity;
        rearmCooldown = 0;

        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                Integer.MAX_VALUE, 255, false, false));
        entity.teleportTo(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.1,
                worldPosition.getZ() + 0.5);
        entity.setNoGravity(false);

        pendingSnapAnim = true;
        triggerAnim("main", "snap");

        if (level instanceof ServerLevel sl) {
            sl.playSound(null, worldPosition,
                    com.harry.wildcraft.init.ModSounds.TRAP_SNAP.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    0.6f, 0.9f + sl.random.nextFloat() * 0.2f);
            level.sendBlockUpdated(worldPosition,
                    getBlockState(), getBlockState(), 3);
        }
        setChanged();
    }

    public void release() {
        if (capturedEntityCache != null && capturedEntityCache.isAlive()) {
            capturedEntityCache.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            capturedEntityCache.setNoGravity(false);
        }
        capturedEntityUUID  = null;
        capturedEntityCache = null;

        rearmCooldown = REARM_COOLDOWN_TICKS;

        pendingOpenAnim = true;
        triggerAnim("main", "open");

        if (level != null) {
            level.sendBlockUpdated(worldPosition,
                    getBlockState(), getBlockState(), 3);
        }
        setChanged();
    }

    public LivingEntity getCapturedEntity() {
        if (capturedEntityCache == null && capturedEntityUUID != null
                && level instanceof ServerLevel sl) {
            var e = sl.getEntity(capturedEntityUUID);
            if (e instanceof LivingEntity le) capturedEntityCache = le;
        }
        return capturedEntityCache;
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (rearmCooldown > 0) {
            rearmCooldown--;
        }

        LivingEntity captured = getCapturedEntity();
        if (captured == null || !captured.isAlive()) {
            if (capturedEntityUUID != null) {
                capturedEntityUUID  = null;
                capturedEntityCache = null;
                if (!getBlockState().getValue(TrapBlock.OPEN)) {
                    level.setBlock(worldPosition,
                            getBlockState().setValue(TrapBlock.OPEN, true), 3);
                    pendingOpenAnim = true;
                    triggerAnim("main", "open");
                    level.sendBlockUpdated(worldPosition,
                            getBlockState(), getBlockState(), 3);
                    rearmCooldown = REARM_COOLDOWN_TICKS;
                }
            }
            return;
        }

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.1;
        double cz = worldPosition.getZ() + 0.5;
        if (captured.distanceToSqr(cx, cy, cz) > 0.5) {
            captured.teleportTo(cx, cy, cz);
        }
        captured.setDeltaMovement(0, 0, 0);
        if (!captured.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            captured.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    Integer.MAX_VALUE, 255, false, false));
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos pos = getBlockPos();
        return new AABB(
                pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5,
                pos.getX() + 1.5, pos.getY() + 1.5, pos.getZ() + 1.5
        );
    }

    @Override
    public net.minecraft.network.protocol.Packet<
            net.minecraft.network.protocol.game.ClientGamePacketListener>
    getUpdatePacket() {
        return net.minecraft.network.protocol.game
                .ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("SnapAnim", pendingSnapAnim);
        tag.putBoolean("OpenAnim", pendingOpenAnim);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.getBoolean("SnapAnim")) {
            triggerAnim("main", "snap");
        }
        if (tag.getBoolean("OpenAnim")) {
            triggerAnim("main", "open");
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (capturedEntityUUID != null)
            tag.putUUID("CapturedEntity", capturedEntityUUID);
        tag.putInt("RearmCooldown", rearmCooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("CapturedEntity"))
            capturedEntityUUID = tag.getUUID("CapturedEntity");
        rearmCooldown = tag.getInt("RearmCooldown");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}