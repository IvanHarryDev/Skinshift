package com.harry.wildcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class TipiBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 16, 2),   // Pared trasera
            Block.box(0, 0, 2, 2, 16, 14),   // Pared izquierda
            Block.box(14, 0, 2, 16, 16, 14), // Pared derecha
            Block.box(0, 14, 2, 16, 16, 16)  // Techo
    );

    public TipiBlock(Properties props) {
        super(props.strength(2.0f).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING,
                ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> rotate(NORTH_SHAPE, net.minecraft.world.level.block.Rotation.CLOCKWISE_180);
            case EAST  -> rotate(NORTH_SHAPE, net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
            case WEST  -> rotate(NORTH_SHAPE, net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);
            default    -> NORTH_SHAPE;
        };
    }

    private static VoxelShape rotate(VoxelShape shape, net.minecraft.world.level.block.Rotation rotation) {
        return Shapes.block();
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TipiBlockEntity(pos, state);
    }
}