package com.harry.wildcraft.init;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.block.TipiBlockEntity;
import com.harry.wildcraft.block.TrapBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WildCraftMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<TrapBlockEntity>> TRAP_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("trap_block_entity",
                    () -> BlockEntityType.Builder
                            .of(TrapBlockEntity::new, ModBlocks.TRAP_BLOCK.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<TipiBlockEntity>> TIPI_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("tipi_block_entity",
                    () -> BlockEntityType.Builder
                            .of(TipiBlockEntity::new, ModBlocks.TIPI.get())
                            .build(null));
}