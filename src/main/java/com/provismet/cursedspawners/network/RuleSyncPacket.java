/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RuleSyncPacket(double miningSpeedModifier) {
    public static void encode(RuleSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.miningSpeedModifier);
    }

    public static RuleSyncPacket decode(FriendlyByteBuf buf) {
        return new RuleSyncPacket(buf.readDouble());
    }

    public static void handle(RuleSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientRuleState.setMiningSpeedModifier(packet.miningSpeedModifier));
        context.setPacketHandled(true);
    }
}
