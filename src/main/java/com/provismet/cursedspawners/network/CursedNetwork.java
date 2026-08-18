/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.network;

import com.provismet.cursedspawners.CursedSpawners;
import com.provismet.cursedspawners.rules.CursedSpawnerRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CursedNetwork {
    private CursedNetwork() {}
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CursedSpawners.id("main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static int id = 0;

    public static void init() {
        CHANNEL.messageBuilder(RuleSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RuleSyncPacket::encode)
                .decoder(RuleSyncPacket::decode)
                .consumerMainThread(RuleSyncPacket::handle)
                .add();
    }

    public static void sync(ServerPlayer player) {
        double modifier = CursedSpawnerRules.get(player.serverLevel()).miningSpeedModifier();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RuleSyncPacket(modifier));
    }

    public static void syncAll(MinecraftServer server) {
        double modifier = CursedSpawnerRules.get(server).miningSpeedModifier();
        CHANNEL.send(PacketDistributor.ALL.noArg(), new RuleSyncPacket(modifier));
    }
}
