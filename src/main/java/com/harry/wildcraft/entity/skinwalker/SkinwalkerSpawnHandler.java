package com.harry.wildcraft.entity.skinwalker;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.init.ModEntities;
import com.harry.wildcraft.util.EntityHelper;
import com.harry.wildcraft.util.SightHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

public class SkinwalkerSpawnHandler {

    private static final long NIGHT_START_TIME = 13000L;
    private static final int SPAWN_DISTANCE = 200;
    private static final int SPAWN_ATTEMPTS = 20;

    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide || event.phase != TickEvent.Phase.END) return;
        ServerLevel level = (ServerLevel) event.level;

        long dayTime = level.getDayTime() % 24000;
        if (dayTime != NIGHT_START_TIME) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;

        for (ServerPlayer player : level.players()) {
            if (!EntityHelper.isInSurvivalOrAdventure(player)) continue;

            if (hasSkinwalkerForPlayerAnyDimension(server, player.getUUID())) continue;

            if (level.random.nextInt(3) != 0) continue;

            spawnSkinwalkerForPlayer(level, player);
        }
    }

    private static boolean hasSkinwalkerForPlayerAnyDimension(MinecraftServer server, UUID playerUUID) {
        for (ServerLevel dim : server.getAllLevels()) {
            boolean found = !dim.getEntitiesOfClass(SkinwalkerEntity.class,
                    new AABB(-30000, dim.getMinBuildHeight(), -30000,
                            30000, dim.getMaxBuildHeight(), 30000),
                    sw -> playerUUID.equals(sw.getTargetPlayerUUID())
            ).isEmpty();
            if (found) return true;
        }
        return false;
    }

    private static void spawnSkinwalkerForPlayer(ServerLevel level, ServerPlayer player) {
        Vec3 pos = SightHelper.findPositionOutOfSight(level, player, SPAWN_DISTANCE, SPAWN_ATTEMPTS);
        if (pos == null) return;

        SkinwalkerEntity sw = ModEntities.SKINWALKER.get().create(level);
        if (sw == null) return;

        sw.setTargetPlayer(player.getUUID());
        sw.setMode(SkinwalkerMode.PASSIVE);
        sw.setModeTimer(0);
        sw.moveTo(pos.x, pos.y, pos.z);

        SkinwalkerMorphHelper.morphInstantToClosestBiomeAnimal(sw, level, pos);

        level.addFreshEntity(sw);
        WildCraftMod.LOGGER.debug("Skinwalker spawned for player {} at {}, {}, {}",
                player.getName().getString(), (int) pos.x, (int) pos.y, (int) pos.z);
    }
}