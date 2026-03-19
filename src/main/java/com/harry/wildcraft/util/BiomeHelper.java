package com.harry.wildcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class BiomeHelper {

    public static String getBiomeCategory(ServerLevel level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        var biome = level.getBiome(blockPos);
        ResourceLocation key = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(biome.value());
        if (key == null) return "default";
        String path = key.getPath();
        if (path.contains("forest"))     return "forest";
        if (path.contains("plains"))     return "plains";
        if (path.contains("desert"))     return "desert";
        if (path.contains("badlands"))   return "badlands";
        if (path.contains("taiga"))      return "taiga";
        if (path.contains("swamp"))      return "swamp";
        if (path.contains("jungle"))     return "jungle";
        if (path.contains("savanna"))    return "savanna";
        if (path.contains("snowy"))      return "snowy";
        if (path.contains("meadow"))     return "meadow";
        return "default";
    }
}