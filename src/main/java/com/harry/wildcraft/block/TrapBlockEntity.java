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

import java.util.UUID;

public class TrapBlockEntity extends BlockEntity {

    private UUID capturedEntityUUID = null;
    private LivingEntity capturedEntityCache = null;

    public TrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAP_BLOCK_ENTITY.get(), pos, state);
    }

    public void captureEntity(LivingEntity entity) {
        capturedEntityUUID  = entity.getUUID();
        capturedEntityCache = entity;
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                Integer.MAX_VALUE, 255, false, false));
        entity.teleportTo(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.1,
                worldPosition.getZ() + 0.5);
        entity.setNoGravity(false);
        setChanged();
    }

    public void release() {
        if (capturedEntityCache != null && capturedEntityCache.isAlive()) {
            capturedEntityCache.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            capturedEntityCache.setNoGravity(false);
        }
        capturedEntityUUID  = null;
        capturedEntityCache = null;
        setChanged();
    }

    public LivingEntity getCapturedEntity() {
        if (capturedEntityCache == null && capturedEntityUUID != null
                && level instanceof ServerLevel sl) {
            net.minecraft.world.entity.Entity e = sl.getEntity(capturedEntityUUID);
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
                if (getBlockState().getValue(TrapBlock.OPEN) == false) {
                    level.setBlock(worldPosition,
                            getBlockState().setValue(TrapBlock.OPEN, true), 3);
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
            captured.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
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
}