package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.init.ModEntities;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

public class SkinwalkerSpawnHandler {
    private static final long NIGHT_START_TIME = 13000L;

    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide || event.phase != TickEvent.Phase.END) return;
        ServerLevel level = (ServerLevel) event.level;
        long dayTime = level.getDayTime() % 24000;
        if (dayTime != NIGHT_START_TIME) return;

        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (hasSkinwalkerForPlayer(level, player.getUUID())) continue;
            if (level.random.nextInt(3) != 0) continue; // 1 en 3
            spawnSkinwalkerForPlayer(level, player);
        }
    }

    private static boolean hasSkinwalkerForPlayer(ServerLevel level, UUID playerUUID) {
        return !level.getEntitiesOfClass(SkinwalkerEntity.class,
                new AABB(-30000, level.getMinBuildHeight(), -30000,
                        30000, level.getMaxBuildHeight(),  30000),
                sw -> playerUUID.equals(sw.getTargetPlayerUUID())
        ).isEmpty();
    }

    private static void spawnSkinwalkerForPlayer(ServerLevel level, ServerPlayer player) {
        Vec3 pos = SightHelper.findPositionOutOfSight(level, player, 200, 20);
        if (pos == null) return;
        SkinwalkerEntity sw = ModEntities.SKINWALKER.get().create(level);
        if (sw == null) return;
        sw.setTargetPlayer(player.getUUID());
        sw.moveTo(pos.x, pos.y, pos.z);
        SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, pos);
        level.addFreshEntity(sw);
        WildCraftMod.LOGGER.debug("Skinwalker spawned for player " + player.getName().getString());
    }
}