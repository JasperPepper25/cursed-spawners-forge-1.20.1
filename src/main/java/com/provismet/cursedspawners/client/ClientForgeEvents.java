/* MODIFIED unofficial Forge 1.20.1 client event bridge; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.client;

import com.provismet.cursedspawners.CursedSpawners;
import com.provismet.cursedspawners.network.ClientRuleState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CursedSpawners.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void playerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // Match the original Fabric PlayerManager mixin: never carry a server's
        // spawner break-speed value into the next client session.
        ClientRuleState.setMiningSpeedModifier(1.0D);
    }
}
