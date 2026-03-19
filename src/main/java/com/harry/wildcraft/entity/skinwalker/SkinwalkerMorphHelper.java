package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.init.ModEntities;
import com.harry.wildcraft.util.BiomeHelper;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.*;

public class SkinwalkerMorphHelper {
    private static final Map<String, List<Supplier<EntityType<?>>>> BIOME_MOBS = new HashMap<>();

    static {
        List<Supplier<EntityType<?>>> common = new ArrayList<>();
        common.add(() -> EntityType.WOLF);
        common.add(() -> EntityType.FOX);
        common.add(() -> EntityType.SHEEP);
        common.add(() -> EntityType.COW);
        common.add(() -> EntityType.PIG);
        common.add(() -> EntityType.RABBIT);
        common.add(() -> EntityType.CAT);
        common.add(ModEntities.DEER::get);
        BIOME_MOBS.put("default", common);
        BIOME_MOBS.put("forest",  common);

        List<Supplier<EntityType<?>>> plains = new ArrayList<>();
        plains.add(() -> EntityType.COW);
        plains.add(() -> EntityType.SHEEP);
        plains.add(ModEntities.BISON::get);
        plains.add(ModEntities.DEER::get);
        BIOME_MOBS.put("plains",  plains);
        BIOME_MOBS.put("meadow",  plains);

        List<Supplier<EntityType<?>>> desert = new ArrayList<>();
        desert.add(() -> EntityType.RABBIT);
        desert.add(ModEntities.GILA_MONSTER::get);
        BIOME_MOBS.put("desert",   desert);

        List<Supplier<EntityType<?>>> badlands = new ArrayList<>();
        badlands.add(() -> EntityType.RABBIT);
        badlands.add(ModEntities.BISON::get);
        BIOME_MOBS.put("badlands", badlands);

        List<Supplier<EntityType<?>>> taiga = new ArrayList<>();
        taiga.add(() -> EntityType.WOLF);
        taiga.add(ModEntities.BLACK_BEAR::get);
        BIOME_MOBS.put("taiga",    taiga);

        List<Supplier<EntityType<?>>> swamp = new ArrayList<>();
        swamp.add(() -> EntityType.FROG);
        swamp.add(() -> EntityType.SHEEP);
        BIOME_MOBS.put("swamp",    swamp);

        List<Supplier<EntityType<?>>> village = new ArrayList<>();
        village.add(() -> EntityType.VILLAGER);
        BIOME_MOBS.put("village",  village);
    }

    public static void morphToClosestBiomeAnimal(SkinwalkerEntity sw,
                                                 ServerLevel level, Vec3 pos) {
        String biomeCategory = BiomeHelper.getBiomeCategory(level, pos);
        List<Supplier<EntityType<?>>> candidates =
                BIOME_MOBS.getOrDefault(biomeCategory, BIOME_MOBS.get("default"));
        EntityType<?> chosen =
                candidates.get(level.random.nextInt(candidates.size())).get();
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