package com.harry.wildcraft.init;

import com.harry.wildcraft.WildCraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WildCraftMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> WILDCRAFT_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wildcraft.main"))
                    .icon(() -> new ItemStack(ModItems.TRAP.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.TRAP.get());
                        output.accept(ModItems.BROOK_TROUT.get());
                        output.accept(ModItems.TIPI_ITEM.get());
                        output.accept(ModItems.SKINWALKER_EGG.get());
                        output.accept(ModItems.GILA_MONSTER_EGG.get());
                        output.accept(ModItems.BLACK_BEAR_EGG.get());
                        output.accept(ModItems.BROOK_TROUT_EGG.get());
                        output.accept(ModItems.BISON_EGG.get());
                        output.accept(ModItems.COYOTE_EGG.get());
                        output.accept(ModItems.DEER_EGG.get());
                        output.accept(ModItems.OWL_EGG.get());
                    })
                    .build());
}