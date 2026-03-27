package com.harry.wildcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import com.harry.wildcraft.init.ModBlocks;

import java.util.Collections;
import java.util.List;

public class TipiPartBlock extends Block {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty OFFSET_X =
            IntegerProperty.create("offset_x", 0, 2);
    public static final IntegerProperty OFFSET_Y =
            IntegerProperty.create("offset_y", 0, 2);
    public static final IntegerProperty OFFSET_Z =
            IntegerProperty.create("offset_z", 0, 2);

    private static final VoxelShape BASE_CORNER_NW = Shapes.or(
            Shapes.box(0, 0, 0, 1, 1, 0.4),
            Shapes.box(0, 0, 0, 0.4, 1, 1)
    );
    private static final VoxelShape BASE_CORNER_NE = Shapes.or(
            Shapes.box(0, 0, 0, 1, 1, 0.4),
            Shapes.box(0.6, 0, 0, 1, 1, 1)
    );
    private static final VoxelShape BASE_CORNER_SW = Shapes.or(
            Shapes.box(0, 0, 0.6, 1, 1, 1),
            Shapes.box(0, 0, 0, 0.4, 1, 1)
    );
    private static final VoxelShape BASE_CORNER_SE = Shapes.or(
            Shapes.box(0, 0, 0.6, 1, 1, 1),
            Shapes.box(0.6, 0, 0, 1, 1, 1)
    );

    private static final VoxelShape BASE_NORTH = Shapes.box(0, 0, 0, 1, 1, 0.3);
    private static final VoxelShape BASE_SOUTH = Shapes.box(0, 0, 0.7, 1, 1, 1);
    private static final VoxelShape BASE_WEST  = Shapes.box(0, 0, 0, 0.3, 1, 1);
    private static final VoxelShape BASE_EAST  = Shapes.box(0.7, 0, 0, 1, 1, 1);

    private static final VoxelShape MID_CORNER_NW = Shapes.or(
            Shapes.box(0.1, 0, 0.1, 0.9, 1, 0.35),
            Shapes.box(0.1, 0, 0.1, 0.35, 1, 0.9)
    );
    private static final VoxelShape MID_CORNER_NE = Shapes.or(
            Shapes.box(0.1, 0, 0.1, 0.9, 1, 0.35),
            Shapes.box(0.65, 0, 0.1, 0.9, 1, 0.9)
    );
    private static final VoxelShape MID_CORNER_SW = Shapes.or(
            Shapes.box(0.1, 0, 0.65, 0.9, 1, 0.9),
            Shapes.box(0.1, 0, 0.1, 0.35, 1, 0.9)
    );
    private static final VoxelShape MID_CORNER_SE = Shapes.or(
            Shapes.box(0.1, 0, 0.65, 0.9, 1, 0.9),
            Shapes.box(0.65, 0, 0.1, 0.9, 1, 0.9)
    );
    private static final VoxelShape MID_NORTH = Shapes.box(0.15, 0, 0.15, 0.85, 1, 0.35);
    private static final VoxelShape MID_SOUTH = Shapes.box(0.15, 0, 0.65, 0.85, 1, 0.85);
    private static final VoxelShape MID_WEST  = Shapes.box(0.15, 0, 0.15, 0.35, 1, 0.85);
    private static final VoxelShape MID_EAST  = Shapes.box(0.65, 0, 0.15, 0.85, 1, 0.85);
    private static final VoxelShape MID_CENTER = Shapes.empty();

    private static final VoxelShape TOP_CORNER = Shapes.box(0.2, 0, 0.2, 0.8, 1, 0.8);
    private static final VoxelShape TOP_EDGE   = Shapes.box(0.25, 0, 0.25, 0.75, 1, 0.75);
    private static final VoxelShape TOP_CENTER = Shapes.box(0.3, 0, 0.3, 0.7, 0.6, 0.7);

    private static final VoxelShape EMPTY = Shapes.empty();

    public TipiPartBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, 1)
                .setValue(OFFSET_Y, 0)
                .setValue(OFFSET_Z, 1));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_X, OFFSET_Y, OFFSET_Z);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext ctx) {
        int ox = state.getValue(OFFSET_X);
        int oy = state.getValue(OFFSET_Y);
        int oz = state.getValue(OFFSET_Z);
        Direction facing = state.getValue(FACING);

        return getShapeForPosition(ox, oy, oz, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return getCollisionShape(state, level, pos, ctx);
    }

    private VoxelShape getShapeForPosition(int ox, int oy, int oz, Direction facing) {
        int relX = ox - 1;
        int relZ = oz - 1;

        boolean isEntrance = isEntrancePosition(relX, relZ, facing);

        if (relX == 0 && relZ == 0) {
            if (oy == 2) return TOP_CENTER;
            return EMPTY;
        }

        if (isEntrance && oy <= 1) {
            return EMPTY;
        }

        boolean isCorner = (relX != 0 && relZ != 0);
        boolean isEdge = !isCorner;

        if (oy == 0) {
            if (isCorner) {
                return getBaseCorner(relX, relZ);
            } else {
                return getBaseEdge(relX, relZ);
            }
        } else if (oy == 1) {
            if (isCorner) {
                return getMidCorner(relX, relZ);
            } else {
                return getMidEdge(relX, relZ);
            }
        } else {
            if (isCorner) return TOP_CORNER;
            if (isEdge) return TOP_EDGE;
            return TOP_CENTER;
        }
    }

    private boolean isEntrancePosition(int relX, int relZ, Direction facing) {
        return switch (facing) {
            case NORTH -> relZ == -1 && relX == 0;
            case SOUTH -> relZ == 1 && relX == 0;
            case EAST  -> relX == 1 && relZ == 0;
            case WEST  -> relX == -1 && relZ == 0;
            default -> false;
        };
    }

    private VoxelShape getBaseCorner(int relX, int relZ) {
        if (relX == -1 && relZ == -1) return BASE_CORNER_NW;
        if (relX == 1 && relZ == -1) return BASE_CORNER_NE;
        if (relX == -1 && relZ == 1) return BASE_CORNER_SW;
        return BASE_CORNER_SE;
    }

    private VoxelShape getBaseEdge(int relX, int relZ) {
        if (relZ == -1) return BASE_NORTH;
        if (relZ == 1) return BASE_SOUTH;
        if (relX == -1) return BASE_WEST;
        return BASE_EAST;
    }

    private VoxelShape getMidCorner(int relX, int relZ) {
        if (relX == -1 && relZ == -1) return MID_CORNER_NW;
        if (relX == 1 && relZ == -1) return MID_CORNER_NE;
        if (relX == -1 && relZ == 1) return MID_CORNER_SW;
        return MID_CORNER_SE;
    }

    private VoxelShape getMidEdge(int relX, int relZ) {
        if (relZ == -1) return MID_NORTH;
        if (relZ == 1) return MID_SOUTH;
        if (relX == -1) return MID_WEST;
        return MID_EAST;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if ((isMoving)) {
                super.onRemove(state, level, pos, newState, isMoving);
                return;
            }

            BlockPos masterPos = findMasterPos(pos, state);
            if (masterPos != null) {
                BlockState masterState = level.getBlockState(masterPos);
                if (masterState.getBlock() instanceof TipiBlock tipi) {
                    Block.popResource(level, masterPos,
                            new ItemStack(ModBlocks.TIPI.get().asItem()));
                    tipi.removeParts(level, masterPos);
                    level.setBlock(masterPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos masterPos = findMasterPos(pos, state);
            if (masterPos != null) {
                BlockState masterState = level.getBlockState(masterPos);
                if (masterState.getBlock() instanceof TipiBlock tipi) {
                    if (!player.isCreative()) {
                        Block.popResource(level, masterPos,
                                new ItemStack(ModBlocks.TIPI.get().asItem()));
                    }
                    tipi.removeParts(level, masterPos);
                    level.setBlock(masterPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private BlockPos findMasterPos(BlockPos partPos, BlockState state) {
        int ox = state.getValue(OFFSET_X) - 1;
        int oy = state.getValue(OFFSET_Y);
        int oz = state.getValue(OFFSET_Z) - 1;
        return partPos.offset(-ox, -oy, -oz);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
                                    net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return Collections.emptyList();
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