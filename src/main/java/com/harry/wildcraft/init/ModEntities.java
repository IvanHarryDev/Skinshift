package com.harry.wildcraft.init;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.*;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WildCraftMod.MOD_ID);

    public static final RegistryObject<EntityType<SkinwalkerEntity>> SKINWALKER =
            ENTITIES.register("skinwalker",
                    () -> EntityType.Builder.<SkinwalkerEntity>of(SkinwalkerEntity::new,
                                    MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(80)
                            .build("skinwalker"));

    public static final RegistryObject<EntityType<GilaMonsterEntity>> GILA_MONSTER =
            ENTITIES.register("gila_monster",
                    () -> EntityType.Builder.<GilaMonsterEntity>of(GilaMonsterEntity::new,
                                    MobCategory.CREATURE)
                            .sized(1.4f, 0.45f)
                            .build("gila_monster"));

    public static final RegistryObject<EntityType<BlackBearEntity>> BLACK_BEAR =
            ENTITIES.register("black_bear",
                    () -> EntityType.Builder.<BlackBearEntity>of(BlackBearEntity::new,
                                    MobCategory.CREATURE)
                            .sized(1.2f, 1.4f)
                            .build("black_bear"));

    public static final RegistryObject<EntityType<BrookTroutEntity>> BROOK_TROUT =
            ENTITIES.register("brook_trout",
                    () -> EntityType.Builder.<BrookTroutEntity>of(BrookTroutEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(0.5f, 0.3f)
                            .build("brook_trout"));

    public static final RegistryObject<EntityType<BisonEntity>> BISON =
            ENTITIES.register("bison",
                    () -> EntityType.Builder.<BisonEntity>of(BisonEntity::new,
                                    MobCategory.CREATURE)
                            .sized(1.8f, 1.6f)
                            .build("bison"));

    public static final RegistryObject<EntityType<CoyoteEntity>> COYOTE =
            ENTITIES.register("coyote",
                    () -> EntityType.Builder.<CoyoteEntity>of(CoyoteEntity::new,
                                    MobCategory.CREATURE)
                            .sized(0.9f, 0.85f)
                            .build("coyote"));

    public static final RegistryObject<EntityType<OwlEntity>> OWL =
            ENTITIES.register("owl",
                    () -> EntityType.Builder.<OwlEntity>of(OwlEntity::new,
                                    MobCategory.AMBIENT)
                            .sized(0.7f, 1.0f)
                            .build("owl"));

    public static final RegistryObject<EntityType<DeerMaleEntity>> DEER_MALE =
            ENTITIES.register("deer_male",
                    () -> EntityType.Builder.<DeerMaleEntity>of(DeerMaleEntity::new,
                                    MobCategory.CREATURE)
                            .sized(0.9f, 1.4f)
                            .build("deer_male"));

    public static final RegistryObject<EntityType<DeerFemaleEntity>> DEER_FEMALE =
            ENTITIES.register("deer_female",
                    () -> EntityType.Builder.<DeerFemaleEntity>of(DeerFemaleEntity::new,
                                    MobCategory.CREATURE)
                            .sized(0.9f, 1.4f)
                            .build("deer_female"));
}