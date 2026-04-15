package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.init.ModEntities;
import com.harry.wildcraft.util.BiomeHelper;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Supplier;

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
        common.add(ModEntities.DEER_MALE::get);
        BIOME_MOBS.put("default", common);
        BIOME_MOBS.put("forest", common);

        List<Supplier<EntityType<?>>> plains = new ArrayList<>();
        plains.add(() -> EntityType.COW);
        plains.add(() -> EntityType.SHEEP);
        plains.add(ModEntities.BISON::get);
        plains.add(ModEntities.DEER_MALE::get);
        BIOME_MOBS.put("plains", plains);
        BIOME_MOBS.put("meadow", plains);

        List<Supplier<EntityType<?>>> desert = new ArrayList<>();
        desert.add(() -> EntityType.RABBIT);
        desert.add(ModEntities.GILA_MONSTER::get);
        BIOME_MOBS.put("desert", desert);

        List<Supplier<EntityType<?>>> badlands = new ArrayList<>();
        badlands.add(() -> EntityType.RABBIT);
        badlands.add(ModEntities.BISON::get);
        BIOME_MOBS.put("badlands", badlands);

        List<Supplier<EntityType<?>>> taiga = new ArrayList<>();
        taiga.add(() -> EntityType.WOLF);
        taiga.add(ModEntities.BLACK_BEAR::get);
        BIOME_MOBS.put("taiga", taiga);

        List<Supplier<EntityType<?>>> swamp = new ArrayList<>();
        swamp.add(() -> EntityType.FROG);
        swamp.add(() -> EntityType.SHEEP);
        BIOME_MOBS.put("swamp", swamp);

        List<Supplier<EntityType<?>>> village = new ArrayList<>();
        village.add(() -> EntityType.VILLAGER);
        BIOME_MOBS.put("village", village);

        List<Supplier<EntityType<?>>> jungle = new ArrayList<>();
        jungle.add(() -> EntityType.PARROT);
        jungle.add(() -> EntityType.OCELOT);
        jungle.add(() -> EntityType.CAT);
        BIOME_MOBS.put("jungle", jungle);

        List<Supplier<EntityType<?>>> savanna = new ArrayList<>();
        savanna.add(() -> EntityType.COW);
        savanna.add(() -> EntityType.SHEEP);
        savanna.add(ModEntities.DEER_MALE::get);
        BIOME_MOBS.put("savanna", savanna);

        List<Supplier<EntityType<?>>> snowy = new ArrayList<>();
        snowy.add(() -> EntityType.WOLF);
        snowy.add(() -> EntityType.FOX);
        snowy.add(() -> EntityType.RABBIT);
        BIOME_MOBS.put("snowy", snowy);
    }

    private static EntityType<?> resolveClosestBiomeAnimal(SkinwalkerEntity sw,
                                                           ServerLevel level, Vec3 pos) {
        String biomeCategory = BiomeHelper.getBiomeCategory(level, pos);
        List<Supplier<EntityType<?>>> candidates =
                BIOME_MOBS.getOrDefault(biomeCategory, BIOME_MOBS.get("default"));

        Set<EntityType<?>> validTypes = new HashSet<>();
        for (Supplier<EntityType<?>> sup : candidates) {
            validTypes.add(sup.get());
        }

        double closestDistSq = Double.MAX_VALUE;
        EntityType<?> closestType = null;

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                sw.getBoundingBox().inflate(64),
                e -> e != sw && e.isAlive() && validTypes.contains(e.getType())
        );

        for (LivingEntity entity : nearby) {
            double distSq = entity.distanceToSqr(pos);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestType = entity.getType();
            }
        }

        if (closestType == null) {
            closestType = candidates.get(level.random.nextInt(candidates.size())).get();
        }

        return closestType;
    }

    public static void morphToClosestBiomeAnimal(SkinwalkerEntity sw,
                                                 ServerLevel level, Vec3 pos) {
        EntityType<?> chosen = resolveClosestBiomeAnimal(sw, level, pos);
        sw.startMorph(chosen.getDescriptionId(), false);
    }

    public static void morphInstantToClosestBiomeAnimal(SkinwalkerEntity sw,
                                                        ServerLevel level, Vec3 pos) {
        EntityType<?> chosen = resolveClosestBiomeAnimal(sw, level, pos);
        sw.morphInstant(chosen.getDescriptionId(), false);
    }

    public static void morphToOwlForFlight(SkinwalkerEntity sw) {
        sw.startMorph(ModEntities.OWL.get().getDescriptionId(), true);
    }

    public static void unmorphToRealForm(SkinwalkerEntity sw) {
        sw.startMorph("none", false);
    }

    public static void unmorphInstant(SkinwalkerEntity sw) {
        sw.morphInstant("none", false);
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