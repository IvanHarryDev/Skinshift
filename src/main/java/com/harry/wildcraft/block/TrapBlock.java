package com.harry.wildcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import com.harry.wildcraft.init.ModSounds;
import java.util.UUID;

public class TrapBlock extends Block implements EntityBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public TrapBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(OPEN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(OPEN);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!state.getValue(OPEN) || !(entity instanceof LivingEntity living)) return;
        if (level.isClientSide) return;
        level.setBlock(pos, state.setValue(OPEN, false), 3);
        level.playSound(null, pos, ModSounds.TRAP_SNAP.get(), SoundSource.BLOCKS, 1f, 0.8f);
        if (living instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setDeltaMovement(Vec3.ZERO);
        } else if (living instanceof Player player) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 10, false, false));
        }
        if (level.getBlockEntity(pos) instanceof TrapBlockEntity trapBE)
            trapBE.setTrappedEntity(living.getUUID());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && !state.getValue(OPEN)) releaseMob(state, level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void releaseMob(BlockState state, Level level, BlockPos pos) {
        level.setBlock(pos, state.setValue(OPEN, true), 3);
        if (!(level instanceof ServerLevel sl)) return;
        if (!(level.getBlockEntity(pos) instanceof TrapBlockEntity be)) return;
        UUID uuid = be.getTrappedEntity();
        if (uuid == null) return;
        Entity e = sl.getEntity(uuid);
        if (e instanceof Mob mob) mob.setNoAi(false);
        else if (e instanceof Player p) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        be.setTrappedEntity(null);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrapBlockEntity(pos, state);
    }
}