package com.harry.wildcraft.network;

import com.harry.wildcraft.WildCraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WildCraftMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        int id = 0;
        CHANNEL.messageBuilder(ShakePacket.class, id++)
                .encoder(ShakePacket::encode)
                .decoder(ShakePacket::decode)
                .consumerMainThread(ShakePacket::handle)
                .add();
    }

    public static void sendShakeToPlayer(ServerPlayer player, float intensity, int ticks) {
        CHANNEL.sendTo(
                new ShakePacket(intensity, ticks),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
}