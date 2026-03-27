package com.harry.wildcraft.client;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.renderer.*;
import com.harry.wildcraft.init.ModBlockEntities;
import com.harry.wildcraft.init.ModEntities;
import com.harry.wildcraft.init.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = WildCraftMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    public static void init(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SKINWALKER.get(),    SkinwalkerRenderer::new);
        event.registerEntityRenderer(ModEntities.GILA_MONSTER.get(),  GilaMonsterRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_BEAR.get(),    BlackBearRenderer::new);
        event.registerEntityRenderer(ModEntities.BROOK_TROUT.get(),   BrookTroutRenderer::new);
        event.registerEntityRenderer(ModEntities.BISON.get(),         BisonRenderer::new);
        event.registerEntityRenderer(ModEntities.COYOTE.get(),        CoyoteRenderer::new);
        event.registerEntityRenderer(ModEntities.DEER_MALE.get(),   DeerMaleRenderer::new);
        event.registerEntityRenderer(ModEntities.DEER_FEMALE.get(), DeerFemaleRenderer::new);
        event.registerEntityRenderer(ModEntities.OWL.get(),           OwlRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TIPI_BLOCK_ENTITY.get(), TipiRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TRAP_BLOCK_ENTITY.get(), TrapRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> {
                    ForgeSpawnEggItem egg = (ForgeSpawnEggItem) stack.getItem();
                    return egg.getColor(tintIndex);
                },
                ModItems.SKINWALKER_EGG.get(),
                ModItems.GILA_MONSTER_EGG.get(),
                ModItems.BLACK_BEAR_EGG.get(),
                ModItems.BROOK_TROUT_EGG.get(),
                ModItems.BISON_EGG.get(),
                ModItems.COYOTE_EGG.get(),
                ModItems.DEER_MALE_EGG.get(),
                ModItems.DEER_FEMALE_EGG.get(),
                ModItems.OWL_EGG.get()
        );
    }
}