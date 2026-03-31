package com.harry.wildcraft.init;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMorphHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;

public class ModCommands {
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("spawnsw")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("passive")
                                .executes(ctx -> spawnSW(ctx, SkinwalkerMode.PASSIVE)))
                        .then(Commands.literal("threat")
                                .executes(ctx -> spawnSW(ctx, SkinwalkerMode.THREATENING)))
                        .then(Commands.literal("agro")
                                .executes(ctx -> spawnSW(ctx, SkinwalkerMode.AGGRESSIVE)))
        );
    }

    private static int spawnSW(CommandContext<CommandSourceStack> ctx,
                               SkinwalkerMode mode) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level   = player.serverLevel();
        SkinwalkerEntity sw = ModEntities.SKINWALKER.get().create(level);
        if (sw == null) return 0;

        sw.setMode(mode);
        sw.setModeLocked(true);
        sw.setTargetPlayer(player.getUUID());

        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(5));
        sw.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);

        if (mode != SkinwalkerMode.AGGRESSIVE) {
            SkinwalkerMorphHelper.morphToClosestBiomeAnimal(sw, level, spawnPos);
        }

        level.addFreshEntity(sw);
        ctx.getSource().sendSuccess(
                () -> Component.literal("Skinwalker spawned in " + mode.name() + " mode (locked)"), false);
        return Command.SINGLE_SUCCESS;
    }
}