package com.harry.wildcraft.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.item.TrapItem;
import com.harry.wildcraft.item.BrookTroutItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WildCraftMod.MOD_ID);

    public static final RegistryObject<TrapItem> TRAP =
            ITEMS.register("trap",
                    () -> new TrapItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> BROOK_TROUT =
            ITEMS.register("brook_trout",
                    () -> new BrookTroutItem(new Item.Properties()));

    public static final RegistryObject<Item> TIPI_ITEM =
            ITEMS.register("tipi",
                    () -> new BlockItem(ModBlocks.TIPI.get(), new Item.Properties()));
}