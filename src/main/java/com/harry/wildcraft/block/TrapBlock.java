package com.harry.wildcraft.block;

import com.harry.wildcraft.init.ModBlockEntities;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class TrapBlock extends BaseEntityBlock {

    public static final BooleanProperty OPEN =
            BooleanProperty.create("open");

    private static final VoxelShape SHAPE =
            Shapes.box(0.0625, 0.0, 0.0625, 0.9375, 0.125, 0.9375);

    public TrapBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(OPEN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrapBlockEntity(pos, state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos,
                             Entity entity) {
        if (level.isClientSide) return;
        if (!state.getValue(OPEN)) return; // ya cerrada
        if (!(entity instanceof LivingEntity living)) return;

        level.setBlock(pos, state.setValue(OPEN, false), 3);
        if (level.getBlockEntity(pos) instanceof TrapBlockEntity trap) {
            trap.captureEntity(living);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
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
    @Nullable
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T>
    getTicker(Level level, BlockState state,
              net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type,
                ModBlockEntities.TRAP_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.tick());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }
}