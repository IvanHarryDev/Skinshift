package com.harry.wildcraft.init;

import com.harry.wildcraft.WildCraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WildCraftMod.MOD_ID);

    public static final RegistryObject<SoundEvent> SKINWALKER_SCREAM =
            SOUNDS.register("skinwalker.scream",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "skinwalker.scream")));

    public static final RegistryObject<SoundEvent> SKINWALKER_MORPH =
            SOUNDS.register("skinwalker.morph",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "skinwalker.morph")));

    public static final RegistryObject<SoundEvent> OWL_HOOT =
            SOUNDS.register("owl.hoot",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "owl.hoot")));

    public static final RegistryObject<SoundEvent> TRAP_SNAP =
            SOUNDS.register("trap.snap",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "trap.snap")));
}