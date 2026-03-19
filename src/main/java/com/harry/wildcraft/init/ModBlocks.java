package com.harry.wildcraft.init;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.block.TipiBlock;
import com.harry.wildcraft.block.TrapBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, WildCraftMod.MOD_ID);

    public static final RegistryObject<TrapBlock> TRAP_BLOCK =
            BLOCKS.register("trap_block",
                    () -> new TrapBlock(BlockBehaviour.Properties.of()
                            .strength(5.0f, 6.0f).noOcclusion()));

    public static final RegistryObject<TipiBlock> TIPI =
            BLOCKS.register("tipi",
                    () -> new TipiBlock(BlockBehaviour.Properties.of()
                            .strength(2.0f).noOcclusion()));
}