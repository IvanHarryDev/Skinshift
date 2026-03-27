package com.harry.wildcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.*;
import com.harry.wildcraft.init.ModBlocks;
import javax.annotation.Nullable;

public class TipiBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape CENTER_OUTLINE = Shapes.block();
    private static final VoxelShape CENTER_COLLISION = Shapes.empty();

    public TipiBlock(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos center = ctx.getClickedPos().relative(ctx.getClickedFace());
        Level level = ctx.getLevel();
        Direction facing = ctx.getHorizontalDirection().getOpposite();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos check = center.offset(dx, 0, dz);
                if (dx == 0 && dz == 0) continue;
                if (!level.getBlockState(check).canBeReplaced(ctx)) {
                    return null;
                }
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos check = center.offset(dx, dy, dz);
                    if (!level.getBlockState(check).canBeReplaced(ctx)) {
                        return null;
                    }
                }
            }
        }

        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos center, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        Block partBlock = ModBlocks.TIPI_PART.get();

        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;

                    BlockPos partPos = center.offset(dx, dy, dz);
                    if (!level.getBlockState(partPos).canBeReplaced()) continue;

                    BlockState partState = partBlock.defaultBlockState()
                            .setValue(TipiPartBlock.FACING, facing)
                            .setValue(TipiPartBlock.OFFSET_X, dx + 1)
                            .setValue(TipiPartBlock.OFFSET_Y, dy)
                            .setValue(TipiPartBlock.OFFSET_Z, dz + 1);

                    level.setBlock(partPos, partState, 3);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeParts(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public void removeParts(Level level, BlockPos center) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    BlockPos partPos = center.offset(dx, dy, dz);
                    BlockState ps = level.getBlockState(partPos);
                    if (ps.getBlock() instanceof TipiPartBlock) {
                        level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 3 | 64);
                    }
                }
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TipiBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext ctx) {
        return CENTER_COLLISION;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return CENTER_OUTLINE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}