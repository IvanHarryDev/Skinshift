package com.harry.wildcraft.block;

import com.harry.wildcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TipiBlockEntity extends BlockEntity {

    public TipiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIPI_BLOCK_ENTITY.get(), pos, state);
    }
}