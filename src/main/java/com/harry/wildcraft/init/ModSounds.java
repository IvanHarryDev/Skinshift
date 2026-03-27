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
            SOUNDS.register("skinwalker_scream",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    WildCraftMod.MOD_ID, "skinwalker.scream")));

    public static final RegistryObject<SoundEvent> SKINWALKER_MORPH =
            SOUNDS.register("skinwalker_morph",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    WildCraftMod.MOD_ID, "skinwalker.morph")));

    // BISON
    public static final RegistryObject<SoundEvent> BISON_DEATH = reg("entity.bison.death");
    public static final RegistryObject<SoundEvent> BISON_HURT  = reg("entity.bison.hurt");
    public static final RegistryObject<SoundEvent> BISON_IDLE  = reg("entity.bison.idle");

    // BLACK BEAR
    public static final RegistryObject<SoundEvent> BLACK_BEAR_DEATH = reg("entity.black_bear.death");
    public static final RegistryObject<SoundEvent> BLACK_BEAR_HURT  = reg("entity.black_bear.hurt");
    public static final RegistryObject<SoundEvent> BLACK_BEAR_IDLE  = reg("entity.black_bear.idle");

    // COYOTE
    public static final RegistryObject<SoundEvent> COYOTE_DEATH = reg("entity.coyote.death");
    public static final RegistryObject<SoundEvent> COYOTE_HURT  = reg("entity.coyote.hurt");
    public static final RegistryObject<SoundEvent> COYOTE_IDLE  = reg("entity.coyote.idle");
    public static final RegistryObject<SoundEvent> COYOTE_STALK = reg("entity.coyote.stalk");

    // DEER
    public static final RegistryObject<SoundEvent> DEER_DEATH = reg("entity.deer.death");
    public static final RegistryObject<SoundEvent> DEER_HURT  = reg("entity.deer.hurt");
    public static final RegistryObject<SoundEvent> DEER_IDLE  = reg("entity.deer.idle");

    // GILA MONSTER
    public static final RegistryObject<SoundEvent> GILA_MONSTER_ATTACK = reg("entity.gila_monster.attack");
    public static final RegistryObject<SoundEvent> GILA_MONSTER_DEATH  = reg("entity.gila_monster.death");
    public static final RegistryObject<SoundEvent> GILA_MONSTER_IDLE   = reg("entity.gila_monster.idle");

    // OWL
    public static final RegistryObject<SoundEvent> OWL_DEATH = reg("entity.owl.death");
    public static final RegistryObject<SoundEvent> OWL_IDLE  = reg("entity.owl.idle");
    public static final RegistryObject<SoundEvent> OWL_FLY   = reg("entity.owl.fly");
    public static final RegistryObject<SoundEvent> OWL_HURT  = reg("entity.owl.hurt");

    // TRAP
    public static final RegistryObject<SoundEvent> TRAP_SNAP = reg("block.trap.snap");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name.replace('.', '_'),
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(
                                WildCraftMod.MOD_ID, name)));
    }
}