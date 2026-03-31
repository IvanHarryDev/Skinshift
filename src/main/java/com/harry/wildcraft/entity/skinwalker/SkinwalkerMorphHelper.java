package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.init.ModEntities;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import com.harry.wildcraft.util.BiomeHelper;

import java.util.*;
import java.util.function.Supplier;

public class SkinwalkerMorphHelper {

    private static final Map<String, List<Supplier<EntityType<?>>>> BIOME_MOBS = new HashMap<>();

    static {
        // ── Default / Forest ──
        List<Supplier<EntityType<?>>> common = new ArrayList<>();
        common.add(() -> EntityType.WOLF);
        common.add(() -> EntityType.FOX);
        common.add(() -> EntityType.SHEEP);
        common.add(() -> EntityType.COW);
        common.add(() -> EntityType.PIG);
        common.add(() -> EntityType.RABBIT);
        common.add(() -> EntityType.CAT);
        common.add(ModEntities.DEER_MALE::get);
        BIOME_MOBS.put("default", common);
        BIOME_MOBS.put("forest",  common);

        // ── Plains / Meadow ──
        List<Supplier<EntityType<?>>> plains = new ArrayList<>();
        plains.add(() -> EntityType.COW);
        plains.add(() -> EntityType.SHEEP);
        plains.add(ModEntities.BISON::get);
        plains.add(ModEntities.DEER_MALE::get);
        BIOME_MOBS.put("plains",  plains);
        BIOME_MOBS.put("meadow",  plains);

        // ── Desert ──
        List<Supplier<EntityType<?>>> desert = new ArrayList<>();
        desert.add(() -> EntityType.RABBIT);
        desert.add(ModEntities.GILA_MONSTER::get);
        BIOME_MOBS.put("desert", desert);

        // ── Badlands ──
        List<Supplier<EntityType<?>>> badlands = new ArrayList<>();
        badlands.add(() -> EntityType.RABBIT);
        badlands.add(ModEntities.BISON::get);
        BIOME_MOBS.put("badlands", badlands);

        // ── Taiga ──
        List<Supplier<EntityType<?>>> taiga = new ArrayList<>();
        taiga.add(() -> EntityType.WOLF);
        taiga.add(ModEntities.BLACK_BEAR::get);
        BIOME_MOBS.put("taiga", taiga);

        // ── Swamp ──
        List<Supplier<EntityType<?>>> swamp = new ArrayList<>();
        swamp.add(() -> EntityType.FROG);
        swamp.add(() -> EntityType.SHEEP);
        BIOME_MOBS.put("swamp", swamp);

        // ── Village ──
        List<Supplier<EntityType<?>>> village = new ArrayList<>();
        village.add(() -> EntityType.VILLAGER);
        BIOME_MOBS.put("village", village);

        // ── Jungle ──
        List<Supplier<EntityType<?>>> jungle = new ArrayList<>();
        jungle.add(() -> EntityType.PARROT);
        jungle.add(() -> EntityType.OCELOT);
        jungle.add(() -> EntityType.CAT);
        BIOME_MOBS.put("jungle", jungle);

        // ── Savanna ──
        List<Supplier<EntityType<?>>> savanna = new ArrayList<>();
        savanna.add(() -> EntityType.COW);
        savanna.add(() -> EntityType.SHEEP);
        savanna.add(ModEntities.DEER_MALE::get);
        BIOME_MOBS.put("savanna", savanna);

        // ── Snowy ──
        List<Supplier<EntityType<?>>> snowy = new ArrayList<>();
        snowy.add(() -> EntityType.WOLF);
        snowy.add(() -> EntityType.FOX);
        snowy.add(() -> EntityType.RABBIT);
        BIOME_MOBS.put("snowy", snowy);
    }

    public static void morphToClosestBiomeAnimal(SkinwalkerEntity sw,
                                                 ServerLevel level, Vec3 pos) {
        String biomeCategory = BiomeHelper.getBiomeCategory(level, pos);
        List<Supplier<EntityType<?>>> candidates =
                BIOME_MOBS.getOrDefault(biomeCategory, BIOME_MOBS.get("default"));
        EntityType<?> chosen = candidates.get(level.random.nextInt(candidates.size())).get();
        sw.setMorphing(true);
        sw.setMorphedInto(chosen.getDescriptionId());
        sw.setMorphed(true);
    }

    public static void morphToOwlIfNoPath(SkinwalkerEntity sw, Player target) {
        Path path = sw.getNavigation().createPath(target, 0);
        if (path == null || !path.canReach()) {
            sw.setMorphedInto(ModEntities.OWL.get().getDescriptionId());
            sw.setMorphed(true);
            sw.setNoGravity(true);
        }
    }

    public static void checkAndMorphIfBiomeChanged(SkinwalkerEntity sw, ServerLevel level) {
        String currentBiome = BiomeHelper.getBiomeCategory(level, sw.position());
        if (!currentBiome.equals(sw.getLastBiome())) {
            Player target = level.getPlayerByUUID(sw.getTargetPlayerUUID());
            if (target == null) return;
            if (!SightHelper.isPlayerLookingAt(target, sw.position(), 0.95)) {
                morphToClosestBiomeAnimal(sw, level, sw.position());
                sw.setLastBiome(currentBiome);
            }
        }
    }
}