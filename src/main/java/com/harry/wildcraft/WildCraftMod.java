package com.harry.wildcraft;

import com.harry.wildcraft.client.ClientSetup;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerSpawnHandler;
import com.harry.wildcraft.init.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

@Mod(WildCraftMod.MOD_ID)
public class WildCraftMod {
    public static final String MOD_ID = "wildcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WildCraftMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTab.TABS.register(modBus);

        GeckoLib.initialize();

        modBus.addListener(this::clientSetup);
        forgeBus.addListener(SkinwalkerSpawnHandler::onWorldTick);
        forgeBus.addListener(ModCommands::registerCommands);
    }

    @OnlyIn(Dist.CLIENT)
    private void clientSetup(final FMLClientSetupEvent event) {
        ClientSetup.init(event);
    }
}