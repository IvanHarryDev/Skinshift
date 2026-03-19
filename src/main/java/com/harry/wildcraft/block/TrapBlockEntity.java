package com.harry.wildcraft.block;

import com.harry.wildcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class TrapBlockEntity extends BlockEntity {
    private UUID trappedEntity;

    public TrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAP.get(), pos, state);
    }

    public UUID getTrappedEntity() { return trappedEntity; }
    public void setTrappedEntity(UUID uuid) { this.trappedEntity = uuid; setChanged(); }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (trappedEntity != null) tag.putUUID("TrappedEntity", trappedEntity);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("TrappedEntity")) trappedEntity = tag.getUUID("TrappedEntity");
    }
}