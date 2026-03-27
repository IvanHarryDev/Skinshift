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
            SOUNDS.register("skinwalker_scream",
                    () -> SoundEvent.createFixedRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MOD_ID, "skinwalker.scream"), 24.0f));

    public static final RegistryObject<SoundEvent> SKINWALKER_MORPH =
            SOUNDS.register("skinwalker_morph",
                    () -> SoundEvent.createFixedRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MOD_ID, "skinwalker.morph"), 16.0f));

    // OWL
    public static final RegistryObject<SoundEvent> OWL_HOOT =
            SOUNDS.register("owl_hoot",
                    () -> SoundEvent.createFixedRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(MOD_ID, "owl.hoot"), 16.0f));

    // BISON
    public static final RegistryObject<SoundEvent> BISON_DEATH = regVariable("entity.bison.death");
    public static final RegistryObject<SoundEvent> BISON_HURT  = regVariable("entity.bison.hurt");
    public static final RegistryObject<SoundEvent> BISON_IDLE  = regVariable("entity.bison.idle");

    // BLACK BEAR
    public static final RegistryObject<SoundEvent> BLACK_BEAR_DEATH = regVariable("entity.black_bear.death");
    public static final RegistryObject<SoundEvent> BLACK_BEAR_HURT  = regVariable("entity.black_bear.hurt");
    public static final RegistryObject<SoundEvent> BLACK_BEAR_IDLE  = regVariable("entity.black_bear.idle");

    // COYOTE
    public static final RegistryObject<SoundEvent> COYOTE_DEATH = regVariable("entity.coyote.death");
    public static final RegistryObject<SoundEvent> COYOTE_HURT  = regVariable("entity.coyote.hurt");
    public static final RegistryObject<SoundEvent> COYOTE_IDLE  = regVariable("entity.coyote.idle");
    public static final RegistryObject<SoundEvent> COYOTE_STALK = regVariable("entity.coyote.stalk");

    // DEER
    public static final RegistryObject<SoundEvent> DEER_DEATH = regVariable("entity.deer.death");
    public static final RegistryObject<SoundEvent> DEER_HURT  = regVariable("entity.deer.hurt");
    public static final RegistryObject<SoundEvent> DEER_IDLE  = regVariable("entity.deer.idle");

    // GILA MONSTER
    public static final RegistryObject<SoundEvent> GILA_MONSTER_ATTACK = regVariable("entity.gila_monster.attack");
    public static final RegistryObject<SoundEvent> GILA_MONSTER_DEATH  = regVariable("entity.gila_monster.death");
    public static final RegistryObject<SoundEvent> GILA_MONSTER_IDLE   = regVariable("entity.gila_monster.idle");

    // OWL
    public static final RegistryObject<SoundEvent> OWL_DEATH = regVariable("entity.owl.death");
    public static final RegistryObject<SoundEvent> OWL_IDLE  = regVariable("entity.owl.idle");
    public static final RegistryObject<SoundEvent> OWL_FLY   = regVariable("entity.owl.fly");
    public static final RegistryObject<SoundEvent> OWL_HURT  = regVariable("entity.owl.hurt");

    // TRAP
    public static final RegistryObject<SoundEvent> TRAP_SNAP = regVariable("block.trap.snap");

    // Helper: VARIABLE range (distance-based attenuation)
    private static RegistryObject<SoundEvent> regVariable(String name) {
        return SOUNDS.register(name.replace('.', '_'),
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, name)));
    }

    @SuppressWarnings("unused")
    private static RegistryObject<SoundEvent> regFixed(String name, float range) {
        return SOUNDS.register(name.replace('.', '_'),
                () -> SoundEvent.createFixedRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, name),
                        range));
    }
}