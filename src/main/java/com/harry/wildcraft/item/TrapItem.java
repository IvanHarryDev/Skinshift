package com.harry.wildcraft.item;

import com.harry.wildcraft.block.TrapBlock;
import com.harry.wildcraft.client.renderer.WildCraftItemRenderer;
import com.harry.wildcraft.init.ModBlocks;
import com.harry.wildcraft.init.ModSounds;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class TrapItem extends Item {
    public TrapItem(Properties props) { super(props); }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return WildCraftItemRenderer.getInstance();
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (level.getBlockState(pos).isAir()) {
            if (!level.isClientSide) {
                level.setBlock(pos,
                        ModBlocks.TRAP_BLOCK.get().defaultBlockState()
                                .setValue(TrapBlock.OPEN, true), 3);
                level.playSound(null, pos, ModSounds.TRAP_SNAP.get(),
                        SoundSource.BLOCKS, 0.6f, 1.2f);
                ctx.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.FAIL;
    }
}