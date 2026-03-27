package com.harry.wildcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.harry.wildcraft.init.ModBlockEntities;
import com.harry.wildcraft.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class TrapBlock extends Block implements EntityBlock {

    public static final BooleanProperty OPEN =
            BooleanProperty.create("open");

    private static final VoxelShape SHAPE =
            Shapes.box(0.1, 0.01, 0.1, 0.9, 0.13, 0.9);

    public TrapBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(OPEN, true));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(ModBlocks.TRAP_BLOCK.get()));
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.2f;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrapBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TrapBlockEntity trap) {
                trap.release();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type,
                ModBlockEntities.TRAP_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.tick());
    }

    @Override
    public void entityInside(BlockState state, Level level,
                             BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!state.getValue(OPEN)) return;
        if (!(entity instanceof LivingEntity living)) return;

        if (entity instanceof Player) return;

        level.setBlock(pos, state.setValue(OPEN, false), 3);
        if (level.getBlockEntity(pos) instanceof TrapBlockEntity trap) {
            trap.captureEntity(living);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level,
                                 BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (!(sl.getBlockEntity(pos) instanceof TrapBlockEntity trap))
            return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        if (held.is(Items.LEAD) && trap.getCapturedEntity() instanceof Mob mob) {
            mob.setLeashedTo(player, true);
            trap.release();
            sl.setBlock(pos, state.setValue(OPEN, true), 3);
            return InteractionResult.SUCCESS;
        }

        trap.release();
        sl.setBlock(pos, state.setValue(OPEN, true), 3);
        return InteractionResult.SUCCESS;
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

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity>
    BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type,
            BlockEntityType<E> targetType,
            BlockEntityTicker<? super E> ticker) {
        return targetType == type ? (BlockEntityTicker<A>) ticker : null;
    }
}