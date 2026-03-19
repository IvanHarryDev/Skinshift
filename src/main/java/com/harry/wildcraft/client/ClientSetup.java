package com.harry.wildcraft.client;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.render.SkinwalkerRenderer;
import com.harry.wildcraft.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
        event.registerEntityRenderer(ModEntities.DEER.get(),          DeerRenderer::new);
        event.registerEntityRenderer(ModEntities.OWL.get(),           OwlRenderer::new);
    }
}