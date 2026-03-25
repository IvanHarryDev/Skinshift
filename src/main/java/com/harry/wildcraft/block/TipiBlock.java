package com.harry.wildcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class TipiBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Shapes.box(0,    0, 0,    0.15, 1, 1   ),
            Shapes.box(0.85, 0, 0,    1,    1, 1   ),
            Shapes.box(0,    0, 0.85, 1,    1, 1   ),
            Shapes.box(0.15, 0.875, 0, 0.85, 1, 0.15)
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Shapes.box(0,    0, 0, 0.15, 1, 1   ),
            Shapes.box(0.85, 0, 0, 1,    1, 1   ),
            Shapes.box(0,    0, 0, 1,    1, 0.15),
            Shapes.box(0.15, 0.875, 0.85, 0.85, 1, 1)
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Shapes.box(0, 0, 0,    1, 1, 0.15),
            Shapes.box(0, 0, 0.85, 1, 1, 1   ),
            Shapes.box(0, 0, 0,    0.15, 1, 1),
            Shapes.box(0.85, 0.875, 0.15, 1, 1, 0.85)
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Shapes.box(0, 0, 0,    1, 1, 0.15),
            Shapes.box(0, 0, 0.85, 1, 1, 1   ),
            Shapes.box(0.85, 0, 0, 1, 1, 1   ),
            Shapes.box(0, 0.875, 0.15, 0.15, 1, 0.85)
    );

    public TipiBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING,
                ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TipiBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    private static VoxelShape buildShape(Direction facing) {
        VoxelShape leftWall, rightWall, backWall, floor;
        floor    = Shapes.box(0, 0, 0, 1, 0.0625, 1);

        leftWall  = Shapes.box(0, 0, 0, 0.15, 1, 1);
        rightWall = Shapes.box(0.85, 0, 0, 1, 1, 1);
        backWall  = Shapes.box(0, 0, 0.85, 1, 1, 1);
        VoxelShape topEntrance = Shapes.box(0.15, 0.875, 0, 0.85, 1, 0.15);

        VoxelShape combined = Shapes.or(floor, leftWall, rightWall, backWall, topEntrance);

        return switch (facing) {
            case SOUTH -> rotateShape(combined, 2);
            case EAST  -> rotateShape(combined, 1);
            case WEST  -> rotateShape(combined, 3);
            default    -> combined;
        };
    }

    private static VoxelShape rotateShape(VoxelShape shape, int steps) {
        return shape;
    }
}