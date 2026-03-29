package com.harry.wildcraft.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import static com.harry.wildcraft.WildCraftMod.MOD_ID;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

    // SKINWALKER
    public static final RegistryObject<SoundEvent> SKINWALKER_SCREAM =
            regFixed("skinwalker.scream", 20.0f);

    public static final RegistryObject<SoundEvent> SKINWALKER_MORPH =
            regFixed("skinwalker.morph", 14.0f);

    // OWL
    public static final RegistryObject<SoundEvent> OWL_HOOT =
            regFixed("owl.hoot", 12.0f);

    // BISON
    public static final RegistryObject<SoundEvent> BISON_DEATH = regVar("entity.bison.death");
    public static final RegistryObject<SoundEvent> BISON_HURT  = regVar("entity.bison.hurt");
    public static final RegistryObject<SoundEvent> BISON_IDLE  = regVar("entity.bison.idle");

    // BLACK BEAR
    public static final RegistryObject<SoundEvent> BLACK_BEAR_DEATH = regVar("entity.black_bear.death");
    public static final RegistryObject<SoundEvent> BLACK_BEAR_HURT  = regVar("entity.black_bear.hurt");
    public static final RegistryObject<SoundEvent> BLACK_BEAR_IDLE  = regVar("entity.black_bear.idle");

    // COYOTE
    public static final RegistryObject<SoundEvent> COYOTE_DEATH = regVar("entity.coyote.death");
    public static final RegistryObject<SoundEvent> COYOTE_HURT  = regVar("entity.coyote.hurt");
    public static final RegistryObject<SoundEvent> COYOTE_IDLE  = regVar("entity.coyote.idle");
    public static final RegistryObject<SoundEvent> COYOTE_STALK = regVar("entity.coyote.stalk");

    // DEER
    public static final RegistryObject<SoundEvent> DEER_DEATH = regVar("entity.deer.death");
    public static final RegistryObject<SoundEvent> DEER_HURT  = regVar("entity.deer.hurt");
    public static final RegistryObject<SoundEvent> DEER_IDLE  = regVar("entity.deer.idle");

    // GILA MONSTER
    public static final RegistryObject<SoundEvent> GILA_MONSTER_ATTACK = regVar("entity.gila_monster.attack");
    public static final RegistryObject<SoundEvent> GILA_MONSTER_DEATH  = regVar("entity.gila_monster.death");
    public static final RegistryObject<SoundEvent> GILA_MONSTER_IDLE   = regVar("entity.gila_monster.idle");

    // OWL
    public static final RegistryObject<SoundEvent> OWL_DEATH = regVar("entity.owl.death");
    public static final RegistryObject<SoundEvent> OWL_IDLE  = regVar("entity.owl.idle");
    public static final RegistryObject<SoundEvent> OWL_FLY   = regVar("entity.owl.fly");
    public static final RegistryObject<SoundEvent> OWL_HURT  = regVar("entity.owl.hurt");

    // TRAP
    public static final RegistryObject<SoundEvent> TRAP_SNAP = regVar("block.trap.snap");

    private static RegistryObject<SoundEvent> regVar(String name) {
        return SOUNDS.register(name.replace('.', '_'),
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, name)));
    }

    private static RegistryObject<SoundEvent> regFixed(String name, float range) {
        return SOUNDS.register(name.replace('.', '_'),
                () -> SoundEvent.createFixedRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, name),
                        range));
    }
}