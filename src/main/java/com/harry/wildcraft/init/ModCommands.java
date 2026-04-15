package com.harry.wildcraft.init;

import com.harry.wildcraft.entity.skinwalker.SkinwalkerDecoyHelper;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMode;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerMorphHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.List;

public class ModCommands {

    private static final List<String> ANIMATIONS = Arrays.asList(
            "idle", "walk", "sprint",
            "walk_enraged", "sprint_enraged",
            "morph", "scream",
            "melee_hurt", "ranged_hurt",
            "weak_attack", "strong_attack",
            "catch_to_drag", "drag",
            "eat", "crouch", "look_around",
            "stop"
    );

    private static final SuggestionProvider<CommandSourceStack> ANIM_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ANIMATIONS, builder);

    private static final int C_GOLD = 0xFFAA00, C_YELLOW = 0xFFFF55, C_GREEN = 0x55FF55,
            C_RED = 0xFF5555, C_AQUA = 0x55FFFF, C_GRAY = 0xAAAAAA,
            C_WHITE = 0xFFFFFF, C_PURPLE = 0xFF55FF;

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("spawnsw").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("passive").executes(ctx -> spawnSW(ctx, SkinwalkerMode.PASSIVE)))
                        .then(Commands.literal("threat").executes(ctx -> spawnSW(ctx, SkinwalkerMode.THREATENING)))
                        .then(Commands.literal("agro").executes(ctx -> spawnSW(ctx, SkinwalkerMode.AGGRESSIVE))));

        event.getDispatcher().register(
                Commands.literal("sw").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("info").executes(ModCommands::showInfo))
                        .then(Commands.literal("kill").executes(ModCommands::killAll))
                        .then(Commands.literal("mode")
                                .then(Commands.literal("passive").executes(ctx -> setMode(ctx, SkinwalkerMode.PASSIVE)))
                                .then(Commands.literal("threat").executes(ctx -> setMode(ctx, SkinwalkerMode.THREATENING)))
                                .then(Commands.literal("agro").executes(ctx -> setMode(ctx, SkinwalkerMode.AGGRESSIVE))))
                        .then(Commands.literal("morph").executes(ModCommands::forceMorph))
                        .then(Commands.literal("unmorph").executes(ModCommands::forceUnmorph))
                        .then(Commands.literal("timer").then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                .executes(ModCommands::setTimer)))
                        .then(Commands.literal("lock").executes(ctx -> setLock(ctx, true)))
                        .then(Commands.literal("unlock").executes(ctx -> setLock(ctx, false)))
                        .then(Commands.literal("tp").executes(ctx -> doTeleport(ctx, 5))
                                .then(Commands.argument("dist", IntegerArgumentType.integer(1, 200))
                                        .executes(ctx -> doTeleport(ctx, IntegerArgumentType.getInteger(ctx, "dist")))))
                        .then(Commands.literal("anim").then(Commands.argument("animation", StringArgumentType.word())
                                .suggests(ANIM_SUGGESTIONS).executes(ModCommands::playAnimFromArg))));
    }

    private static int spawnSW(CommandContext<CommandSourceStack> ctx, SkinwalkerMode mode) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        SkinwalkerEntity sw = ModEntities.SKINWALKER.get().create(level);
        if (sw == null) return 0;
        sw.setMode(mode);
        sw.setModeLocked(true);
        sw.setTargetPlayer(player.getUUID());
        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(5));
        sw.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
        if (mode != SkinwalkerMode.AGGRESSIVE)
            SkinwalkerMorphHelper.morphInstantToClosestBiomeAnimal(sw, level, spawnPos);
        level.addFreshEntity(sw);
        ctx.getSource().sendSuccess(() -> col("SW spawned: ", C_GRAY).append(col(mode.name(), C_YELLOW)), false);
        return Command.SINGLE_SUCCESS;
    }

    // ═══════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═══════════════════════════════════════════════════════════
    private static int playAnimFromArg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String animName = StringArgumentType.getString(ctx, "animation");
        if (!ANIMATIONS.contains(animName)) {
            ctx.getSource().sendFailure(col("Unknown: " + animName, C_RED));
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SkinwalkerEntity sw = findNearest(player);
        if (sw == null) { ctx.getSource().sendFailure(col("No SW nearby.", C_RED)); return 0; }

        resetAllAnimFlags(sw);
        sw.getNavigation().stop();
        sw.setDeltaMovement(0, 0, 0);

        if (sw.isMorphed()) sw.morphInstant("none", false);

        sw.setModeLocked(true);

        if (animName.equals("stop")) {
            ctx.getSource().sendSuccess(() -> col("▶ idle", C_GREEN), false);
            return Command.SINGLE_SUCCESS;
        }

        switch (animName) {
            case "idle":          break;
            case "walk":          forceMove(sw, 0.05); break;
            case "sprint":        forceMove(sw, 0.3); break;
            case "walk_enraged":  sw.setMode(SkinwalkerMode.AGGRESSIVE); forceMove(sw, 0.05); break;
            case "sprint_enraged":sw.setMode(SkinwalkerMode.AGGRESSIVE); forceMove(sw, 0.3); break;
            case "morph":         sw.setMorphing(true); break;
            case "scream":        sw.setScreaming(true); break;
            case "melee_hurt":    sw.setMeleeHurt(true); break;
            case "ranged_hurt":   sw.setRangedHurt(true); break;
            case "weak_attack":   sw.setWeakAttacking(true); break;
            case "strong_attack": sw.setStrongAttacking(true); break;
            case "catch_to_drag":
            case "drag":          sw.setDraggingPlayer(true); break;
            case "eat":           sw.setEating(true); break;
            case "crouch":        sw.setCrouchingAnim(true); break;
            case "look_around":   sw.setLookingAround(true); break;
        }

        final String name = animName;
        ctx.getSource().sendSuccess(() -> col("▶ ", C_GREEN).append(col(name, C_AQUA)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void forceMove(SkinwalkerEntity sw, double speed) {
        Vec3 dir = sw.getLookAngle().normalize();
        sw.setDeltaMovement(dir.x * speed, 0, dir.z * speed);
    }

    private static void resetAllAnimFlags(SkinwalkerEntity sw) {
        sw.setMorphing(false);
        sw.setScreaming(false);
        sw.setMeleeHurt(false);
        sw.setRangedHurt(false);
        sw.setWeakAttacking(false);
        sw.setStrongAttacking(false);
        sw.setDraggingPlayer(false);
        sw.setEating(false);
        sw.setCrouchingAnim(false);
        sw.setLookingAround(false);
    }

    // ═══════════════════════════════════════════════════════════
    //  INFO
    // ═══════════════════════════════════════════════════════════
    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<SkinwalkerEntity> allSWs = findAll(player.serverLevel());
        if (allSWs.isEmpty()) { ctx.getSource().sendFailure(col("No SWs in world.", C_RED)); return 0; }

        send(ctx, col("━━━━━━━ SKINWALKERS: " + allSWs.size() + " ━━━━━━━", C_GOLD));
        for (int i = 0; i < allSWs.size(); i++) {
            SkinwalkerEntity sw = allSWs.get(i);
            double dist = sw.distanceTo(player);
            SkinwalkerMode mode = sw.getMode();
            int mc = mode == SkinwalkerMode.PASSIVE ? C_GREEN : mode == SkinwalkerMode.THREATENING ? C_YELLOW : C_RED;
            String tMax = mode == SkinwalkerMode.PASSIVE ? String.valueOf(SkinwalkerEntity.PASSIVE_DURATION)
                    : mode == SkinwalkerMode.THREATENING ? String.valueOf(SkinwalkerEntity.THREATENING_DURATION) : "∞";

            send(ctx, col(" ┌─ SW #" + (i + 1) + " ", C_GOLD).append(col("ID:" + sw.getId(), C_WHITE)));
            send(ctx, line(" │ Mode", mode.name(), mc));
            send(ctx, line(" │ Timer", sw.getModeTimer() + "/" + tMax, C_WHITE));
            send(ctx, line(" │ Locked", bool(sw.isModeLocked()), sw.isModeLocked() ? C_RED : C_GREEN));
            send(ctx, line(" │ HP", String.format("%.0f/%.0f", sw.getHealth(), sw.getMaxHealth()),
                    sw.getHealth() < 50 ? C_RED : C_GREEN));
            send(ctx, line(" │ Morphed", bool(sw.isMorphed()) + (sw.isMorphed() ? " → " + sw.getMorphedInto() : ""),
                    sw.isMorphed() ? C_AQUA : C_GRAY));
            if (sw.getCurrentDecoy() != null)
                send(ctx, line(" │ Decoy", "ID:" + sw.getCurrentDecoy().getId() +
                        (sw.getCurrentDecoy().isAlive() ? " alive" : " dead"), C_PURPLE));

            StringBuilder ab = new StringBuilder();
            if (sw.isMorphing()) ab.append("morph ");
            if (sw.isScreaming()) ab.append("scream ");
            if (sw.isMeleeHurt()) ab.append("melee_hurt ");
            if (sw.isRangedHurt()) ab.append("ranged_hurt ");
            if (sw.isWeakAttacking()) ab.append("weak_atk ");
            if (sw.isStrongAttacking()) ab.append("strong_atk ");
            if (sw.isDraggingPlayer()) ab.append("drag ");
            if (sw.isEating()) ab.append("eat ");
            if (sw.isCrouchingAnim()) ab.append("crouch ");
            if (sw.isLookingAround()) ab.append("look_around ");
            send(ctx, line(" │ Anim", ab.length() > 0 ? ab.toString().trim() : "idle/move", C_AQUA));
            send(ctx, line(" └ Pos", String.format("%.0f,%.0f,%.0f (%.0fb)", sw.getX(), sw.getY(), sw.getZ(), dist), C_WHITE));
            if (i < allSWs.size() - 1) send(ctx, col("", C_GRAY));
        }
        send(ctx, col("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", C_GOLD));
        return Command.SINGLE_SUCCESS;
    }

    // ═══════════════════════════════════════════════════════════
    //  KILL
    // ═══════════════════════════════════════════════════════════
    private static int killAll(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        List<SkinwalkerEntity> sws = findAll(level);
        int sc = sws.size();
        for (SkinwalkerEntity sw : sws) sw.forceKill();
        int dc = SkinwalkerDecoyHelper.killAllDecoys(level);
        ctx.getSource().sendSuccess(() -> col("Killed: " + sc + " SW + " + dc + " decoys", C_RED), false);
        return sc + dc;
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, SkinwalkerMode mode) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        SkinwalkerEntity sw = findNearest(p);
        if (sw == null) { ctx.getSource().sendFailure(col("No SW.", C_RED)); return 0; }
        sw.setMode(mode); sw.setModeTimer(0);
        resetAllAnimFlags(sw);
        ctx.getSource().sendSuccess(() -> col("Mode → ", C_GRAY).append(col(mode.name(), C_YELLOW)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceMorph(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        SkinwalkerEntity sw = findNearest(p);
        if (sw == null) { ctx.getSource().sendFailure(col("No SW.", C_RED)); return 0; }
        resetAllAnimFlags(sw);
        SkinwalkerMorphHelper.morphInstantToClosestBiomeAnimal(sw, p.serverLevel(), sw.position());
        ctx.getSource().sendSuccess(() -> col("Morphed", C_GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceUnmorph(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        SkinwalkerEntity sw = findNearest(p);
        if (sw == null) { ctx.getSource().sendFailure(col("No SW.", C_RED)); return 0; }
        resetAllAnimFlags(sw);
        SkinwalkerMorphHelper.unmorphInstant(sw);
        ctx.getSource().sendSuccess(() -> col("Unmorphed", C_GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setTimer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        SkinwalkerEntity sw = findNearest(p);
        if (sw == null) { ctx.getSource().sendFailure(col("No SW.", C_RED)); return 0; }
        int t = IntegerArgumentType.getInteger(ctx, "ticks"); sw.setModeTimer(t);
        ctx.getSource().sendSuccess(() -> col("Timer → " + t, C_WHITE), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLock(CommandContext<CommandSourceStack> ctx, boolean lock) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        SkinwalkerEntity sw = findNearest(p);
        if (sw == null) { ctx.getSource().sendFailure(col("No SW.", C_RED)); return 0; }
        sw.setModeLocked(lock);
        ctx.getSource().sendSuccess(() -> col("Timer " + (lock ? "LOCKED" : "UNLOCKED"), lock ? C_RED : C_GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int doTeleport(CommandContext<CommandSourceStack> ctx, int dist) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        SkinwalkerEntity sw = findNearest(p);
        if (sw == null) { ctx.getSource().sendFailure(col("No SW.", C_RED)); return 0; }
        Vec3 look = p.getLookAngle().normalize();
        sw.teleportTo(p.getX() + look.x * dist, p.getY(), p.getZ() + look.z * dist);
        ctx.getSource().sendSuccess(() -> col("TP " + dist + "b", C_GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static SkinwalkerEntity findNearest(ServerPlayer player) {
        List<SkinwalkerEntity> n = findAll(player.serverLevel());
        if (n.isEmpty()) return null;
        n.sort((a, b) -> Double.compare(a.distanceTo(player), b.distanceTo(player)));
        return n.get(0);
    }

    private static List<SkinwalkerEntity> findAll(ServerLevel level) {
        return level.getEntitiesOfClass(SkinwalkerEntity.class,
                new AABB(-30000, level.getMinBuildHeight(), -30000, 30000, level.getMaxBuildHeight(), 30000));
    }

    private static MutableComponent col(String t, int c) { return Component.literal(t).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(c))); }
    private static MutableComponent line(String l, String v, int c) { return col(l + ": ", C_GRAY).append(col(v, c)); }
    private static String bool(boolean v) { return v ? "✔" : "✘"; }
    private static void send(CommandContext<CommandSourceStack> ctx, Component m) { ctx.getSource().sendSuccess(() -> m, false); }
}