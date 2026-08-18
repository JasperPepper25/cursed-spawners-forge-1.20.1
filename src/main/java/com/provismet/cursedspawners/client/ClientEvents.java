/* MODIFIED unofficial Forge 1.20.1 client registration; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.client;

import com.provismet.cursedspawners.CursedSpawners;
import com.provismet.cursedspawners.client.model.SpawnerMimicModel;
import com.provismet.cursedspawners.client.particle.AOEChargingParticle;
import com.provismet.cursedspawners.client.particle.BoostParticle;
import com.provismet.cursedspawners.client.particle.HealParticle;
import com.provismet.cursedspawners.client.renderer.SpawnerMimicRenderer;
import com.provismet.cursedspawners.registry.ModRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CursedSpawners.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    public static final ModelLayerLocation SPAWNER_MIMIC_LAYER =
            new ModelLayerLocation(CursedSpawners.id("spawner_mimic"), "main");

    private ClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.SPAWNER_MIMIC.get(), SpawnerMimicRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SPAWNER_MIMIC_LAYER, SpawnerMimicModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModRegistry.AOE_CHARGING.get(), AOEChargingParticle.Provider::new);
        event.registerSpriteSet(ModRegistry.HEAL.get(), HealParticle.Provider::new);
        event.registerSpriteSet(ModRegistry.BOOST.get(), BoostParticle.Provider::new);
    }
}
