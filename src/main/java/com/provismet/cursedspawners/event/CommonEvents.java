/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.event;

import com.provismet.cursedspawners.CursedSpawners;
import com.provismet.cursedspawners.entity.SpawnerMimicEntity;
import com.provismet.cursedspawners.network.ClientRuleState;
import com.provismet.cursedspawners.network.CursedNetwork;
import com.provismet.cursedspawners.registry.ModRegistry;
import com.provismet.cursedspawners.rules.CursedSpawnerRules;
import com.provismet.cursedspawners.spawner.CursedSpawnerData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CursedSpawners.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CursedSpawnerRules.registerCommands(event);
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CursedNetwork.sync(player);
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CursedNetwork.sync(player);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getState().is(Blocks.SPAWNER)) return;
        Player player = event.getEntity();
        double modifier;
        if (player.level() instanceof ServerLevel serverLevel) {
            modifier = CursedSpawnerRules.get(serverLevel).miningSpeedModifier();
        } else {
            modifier = ClientRuleState.miningSpeedModifier();
        }
        event.setNewSpeed((float)(event.getNewSpeed() * modifier));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void spawnerBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(Blocks.SPAWNER) || !(event.getLevel() instanceof ServerLevel level)) return;
        Player breaker = event.getPlayer();
        if (breaker.isCreative() || breaker.isSpectator()) return;

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawner) || !(blockEntity instanceof CursedSpawnerData data)) return;

        if (!data.cursedSpawners$attemptBreak(level, pos)) {
            event.setCanceled(true);
            return;
        }

        double perSpawnerChance = data.cursedSpawners$getMimicChance();
        double mimicChance = perSpawnerChance == CursedSpawnerData.PASSTHROUGH_MIMIC_CHANCE
                ? CursedSpawnerRules.get(level).mimicChance()
                : Math.max(0.0D, Math.min(1.0D, perSpawnerChance));

        if (level.random.nextDouble() <= mimicChance) {
            CompoundTag tag = spawner.saveWithFullMetadata();
            backportMimicTiming(tag);
            SpawnerMimicEntity mimic = ModRegistry.SPAWNER_MIMIC.get().create(level);
            if (mimic != null) {
                mimic.loadSpawnerData(tag);
                mimic.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        level.random.nextFloat() * 360.0F, 0.0F);
                mimic.setLastHurtByMob(breaker);
                if (level.addFreshEntity(mimic)) {
                    event.setExpToDrop(0);
                    return;
                }
            }
        }

        data.cursedSpawners$dropRewardLoot(level, pos);
    }

    private static void backportMimicTiming(CompoundTag tag) {
        // Match the original 1.21 conversion exactly: only rewrite keys that
        // actually existed on the source spawner, and do not add clamping.
        if (tag.contains("MinSpawnDelay", Tag.TAG_ANY_NUMERIC)) {
            tag.putShort("MinSpawnDelay", (short)((int)(tag.getShort("MinSpawnDelay") / 1.5D)));
        }
        if (tag.contains("MaxSpawnDelay", Tag.TAG_ANY_NUMERIC)) {
            tag.putShort("MaxSpawnDelay", (short)((int)(tag.getShort("MaxSpawnDelay") / 1.5D)));
        }
        if (tag.contains("Delay", Tag.TAG_ANY_NUMERIC)) {
            tag.putShort("Delay", (short)20);
        }
    }
}
