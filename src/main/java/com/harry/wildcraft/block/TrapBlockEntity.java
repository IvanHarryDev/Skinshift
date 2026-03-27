package com.harry.wildcraft.block;

import com.harry.wildcraft.init.ModBlockEntities;
import com.harry.wildcraft.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    public TrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAP_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "main", 0,
                        state -> PlayState.STOP)
                        .triggerableAnim("snap",
                                RawAnimation.begin().thenPlay("animation.trap_block.snap"))
                        .triggerableAnim("open",
                                RawAnimation.begin().thenPlay("animation.trap_block.open"))
        );
    }

    public void captureEntity(LivingEntity entity) {
        capturedEntityUUID  = entity.getUUID();
        capturedEntityCache = entity;
        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                Integer.MAX_VALUE, 255, false, false));
        entity.teleportTo(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.1,
                worldPosition.getZ() + 0.5);
        entity.setNoGravity(false);
        triggerAnim("main", "snap");
        if (level instanceof ServerLevel sl) {
            sl.playSound(null,
                    worldPosition,
                    ModSounds.TRAP_SNAP.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0f, 1.0f);
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
        triggerAnim("main", "open");
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
        LivingEntity captured = getCapturedEntity();
        if (captured == null || !captured.isAlive()) {
            if (capturedEntityUUID != null) {
                capturedEntityUUID  = null;
                capturedEntityCache = null;
                if (!getBlockState().getValue(TrapBlock.OPEN)) {
                    level.setBlock(worldPosition,
                            getBlockState().setValue(TrapBlock.OPEN, true), 3);
                    triggerAnim("main", "open");
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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (capturedEntityUUID != null)
            tag.putUUID("CapturedEntity", capturedEntityUUID);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("CapturedEntity"))
            capturedEntityUUID = tag.getUUID("CapturedEntity");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}