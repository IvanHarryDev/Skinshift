package com.harry.wildcraft.init;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.item.BrookTroutItem;
import com.harry.wildcraft.item.TrapItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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

    // ===== SPAWN EGGS =====
    public static final RegistryObject<ForgeSpawnEggItem> SKINWALKER_EGG =
            ITEMS.register("skinwalker_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SKINWALKER,
                            0x1A1A1A, 0x8B0000,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> GILA_MONSTER_EGG =
            ITEMS.register("gila_monster_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.GILA_MONSTER,
                            0xF4A460, 0x8B4513,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BLACK_BEAR_EGG =
            ITEMS.register("black_bear_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.BLACK_BEAR,
                            0x1C1C1C, 0x5C4033,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BROOK_TROUT_EGG =
            ITEMS.register("brook_trout_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.BROOK_TROUT,
                            0x4682B4, 0xFF6347,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> BISON_EGG =
            ITEMS.register("bison_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.BISON,
                            0x5C4033, 0x3B2314,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> COYOTE_EGG =
            ITEMS.register("coyote_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.COYOTE,
                            0xC4A35A, 0x7A5C28,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> OWL_EGG =
            ITEMS.register("owl_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.OWL,
                            0x8B7355, 0xF5DEB3,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> DEER_MALE_EGG =
            ITEMS.register("deer_male_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.DEER_MALE,
                            0xC68642, 0xFFD700,
                            new Item.Properties()));

    public static final RegistryObject<ForgeSpawnEggItem> DEER_FEMALE_EGG =
            ITEMS.register("deer_female_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.DEER_FEMALE,
                            0xC68642, 0xFFF8DC,
                            new Item.Properties()));
}