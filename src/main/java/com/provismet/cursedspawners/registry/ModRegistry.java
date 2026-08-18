/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.registry;

import com.provismet.cursedspawners.CursedSpawners;
import com.provismet.cursedspawners.entity.SpawnerMimicEntity;
import com.provismet.cursedspawners.particle.AOEChargingParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.core.particles.ParticleType;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CursedSpawners.NAMESPACE);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CursedSpawners.NAMESPACE);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CursedSpawners.NAMESPACE);
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CursedSpawners.NAMESPACE);

    public static final RegistryObject<EntityType<SpawnerMimicEntity>> SPAWNER_MIMIC = ENTITY_TYPES.register(
            "spawner_mimic",
            () -> EntityType.Builder.<SpawnerMimicEntity>of(SpawnerMimicEntity::new, MobCategory.MONSTER)
                    .sized(1.25F, 1.25F)
                    .clientTrackingRange(10)
                    .build(CursedSpawners.id("spawner_mimic").toString())
    );

    public static final RegistryObject<Item> MIMIC_SPAWN_EGG = ITEMS.register(
            "mimic_spawn_egg",
            () -> new ForgeSpawnEggItem(SPAWNER_MIMIC, 0x2A4455, 0x6E0453, new Item.Properties())
    );

    public static final RegistryObject<ParticleType<AOEChargingParticleOptions>> AOE_CHARGING = PARTICLES.register(
            "aoe_charging_indicator",
            () -> new ParticleType<AOEChargingParticleOptions>(false, AOEChargingParticleOptions.DESERIALIZER) {
                @Override
                public Codec<AOEChargingParticleOptions> codec() {
                    return AOEChargingParticleOptions.CODEC;
                }
            }
    );

    public static final RegistryObject<SimpleParticleType> HEAL = PARTICLES.register(
            "heal", () -> new SimpleParticleType(false)
    );
    public static final RegistryObject<SimpleParticleType> BOOST = PARTICLES.register(
            "boost", () -> new SimpleParticleType(false)
    );

    public static final RegistryObject<SoundEvent> ENTITY_MIMIC_AMBIENT = sound("entity.mimic.ambient");
    public static final RegistryObject<SoundEvent> ENTITY_MIMIC_HURT = sound("entity.mimic.hurt");
    public static final RegistryObject<SoundEvent> ENTITY_MIMIC_DEATH = sound("entity.mimic.death");
    public static final RegistryObject<SoundEvent> ENTITY_MIMIC_STEP = sound("entity.mimic.step");
    public static final RegistryObject<SoundEvent> ENTITY_MIMIC_ATTACK = sound("entity.mimic.attack");
    public static final RegistryObject<SoundEvent> BLOCK_SPAWNER_KNOCKBACK = fixedSound("block.spawner.knockback", 8.0F);
    public static final RegistryObject<SoundEvent> BLOCK_SPAWNER_HEAL = fixedSound("block.spawner.heal", 8.0F);
    public static final RegistryObject<SoundEvent> BLOCK_SPAWNER_BOOST = fixedSound("block.spawner.boost", 8.0F);

    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(CursedSpawners.id(id)));
    }

    private static RegistryObject<SoundEvent> fixedSound(String id, float range) {
        return SOUNDS.register(id, () -> SoundEvent.createFixedRangeEvent(CursedSpawners.id(id), range));
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        PARTICLES.register(bus);
    }

    @Mod.EventBusSubscriber(modid = CursedSpawners.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(SPAWNER_MIMIC.get(), SpawnerMimicEntity.createAttributes().build());
        }

        @SubscribeEvent
        public static void creativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(MIMIC_SPAWN_EGG.get());
            }
        }
    }
}
