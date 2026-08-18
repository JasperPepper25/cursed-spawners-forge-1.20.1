/*
 * MODIFIED from Cursed Spawners by Provismet (tag 1.0.0-mc1.21).
 * Unofficial native Forge 1.20.1 backport. See NOTICE and LICENSE.
 */
package com.provismet.cursedspawners;

import com.mojang.logging.LogUtils;
import com.provismet.cursedspawners.network.CursedNetwork;
import com.provismet.cursedspawners.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CursedSpawners.MODID)
public final class CursedSpawners {
    /** Forge loader ids cannot retain the Fabric hyphenated id on 1.20.1. */
    public static final String MODID = "cursed_spawners";
    /** Resource/registry namespace intentionally retained from the original mod. */
    public static final String NAMESPACE = "cursed-spawners";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CursedSpawners() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CursedNetwork::init);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(NAMESPACE, path);
    }
}
