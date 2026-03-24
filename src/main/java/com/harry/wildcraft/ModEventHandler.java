package com.harry.wildcraft;

import com.harry.wildcraft.entity.*;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.init.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WildCraftMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventHandler {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SKINWALKER.get(),    SkinwalkerEntity.createAttributes().build());
        event.put(ModEntities.GILA_MONSTER.get(),  GilaMonsterEntity.createAttributes().build());
        event.put(ModEntities.BLACK_BEAR.get(),    BlackBearEntity.createAttributes().build());
        event.put(ModEntities.BROOK_TROUT.get(),   BrookTroutEntity.createAttributes().build());
        event.put(ModEntities.BISON.get(),         BisonEntity.createAttributes().build());
        event.put(ModEntities.COYOTE.get(),        CoyoteEntity.createAttributes().build());
        event.put(ModEntities.DEER_MALE.get(),   DeerMaleEntity.createAttributes().build());
        event.put(ModEntities.DEER_FEMALE.get(), DeerMaleEntity.createAttributes().build());
        event.put(ModEntities.OWL.get(),           OwlEntity.createAttributes().build());
    }
}