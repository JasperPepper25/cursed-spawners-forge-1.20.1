/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.event;

import com.provismet.cursedspawners.CursedSpawners;
import com.provismet.cursedspawners.entity.SpawnerMimicEntity;
import com.provismet.cursedspawners.mixin.SpawnerMimicAccessor;
import com.provismet.cursedspawners.network.ClientRuleState;
import com.provismet.cursedspawners.network.CursedNetwork;
import com.provismet.cursedspawners.registry.ModRegistry;
import com.provismet.cursedspawners.rules.CursedSpawnerRules;
import com.provismet.cursedspawners.spawner.CursedSpawnerData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CursedSpawners.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEvents {
    private static final String DIFFICULT_SPAWNERS_NAMESPACE = "difficult_spawners";
    private static final String DEFERRED_SPIRITS_KEY = "CursedSpawnersDeferredDifficultSpirits";
    private static final ThreadLocal<BreakContext> BREAK_CONTEXT = new ThreadLocal<>();

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

    /**
     * Runs before compatibility mods such as Difficult Spawners. Reforge attempts are
     * cancelled here so downstream break listeners do not mistake them for a real break.
     * For a final break, the mimic roll is also resolved here so any Evil Spirits spawned
     * by Difficult Spawners during this same event can be captured before entering the world.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void prepareSpawnerBreak(BlockEvent.BreakEvent event) {
        BREAK_CONTEXT.remove();
        if (event.isCanceled() || !event.getState().is(Blocks.SPAWNER)
                || !(event.getLevel() instanceof ServerLevel level)) return;

        Player breaker = event.getPlayer();
        if (breaker.isCreative() || breaker.isSpectator()) return;

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawner) || !(blockEntity instanceof CursedSpawnerData data)) return;

        if (!data.cursedSpawners$attemptBreak(level, pos)) {
            BREAK_CONTEXT.set(BreakContext.reforge(level, pos));
            event.setCanceled(true);
            return;
        }

        double perSpawnerChance = data.cursedSpawners$getMimicChance();
        double mimicChance = perSpawnerChance == CursedSpawnerData.PASSTHROUGH_MIMIC_CHANCE
                ? CursedSpawnerRules.get(level).mimicChance()
                : Math.max(0.0D, Math.min(1.0D, perSpawnerChance));
        boolean createMimic = level.random.nextDouble() <= mimicChance;
        CompoundTag spawnerTag = createMimic ? spawner.saveWithFullMetadata() : null;
        BREAK_CONTEXT.set(BreakContext.finalBreak(level, pos, createMimic, spawnerTag));
    }

    /**
     * Difficult Spawners 1.1.0 creates its Evil Spirits while processing a spawner
     * break. When Cursed Spawners is cancelling a reforge, suppress those entities.
     * When the final break becomes a Mimic, capture their full NBT and defer them until
     * that Mimic dies. Normal final breaks are deliberately left untouched.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isDifficultSpawnerSpirit(event.getEntity())) return;
        BreakContext context = BREAK_CONTEXT.get();
        if (context == null || context.level != level || !context.suppressesSpirits()) return;
        if (context.pos.distSqr(event.getEntity().blockPosition()) > 25.0D) return;

        if (context.deferSpirits) {
            CompoundTag spiritTag = new CompoundTag();
            if (event.getEntity().save(spiritTag)) {
                context.deferredSpirits.add(spiritTag);
            }
        }
        event.setCanceled(true);
    }

    /** Finish Mimic conversion after all normal break listeners have had their turn. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void finishSpawnerBreak(BlockEvent.BreakEvent event) {
        BreakContext context = BREAK_CONTEXT.get();
        if (context == null) return;

        try {
            if (!(event.getLevel() instanceof ServerLevel level)
                    || context.level != level
                    || !context.pos.equals(event.getPos())) return;
            if (event.isCanceled() || !context.finalBreak || !context.createMimic || context.spawnerTag == null) return;

            CompoundTag tag = context.spawnerTag.copy();
            makeMimicTwiceAsFast(tag);
            SpawnerMimicEntity mimic = ModRegistry.SPAWNER_MIMIC.get().create(level);
            if (mimic == null) {
                releaseSpiritTags(level, context.pos.getX() + 0.5D, context.pos.getY(),
                        context.pos.getZ() + 0.5D, context.deferredSpirits);
                return;
            }

            mimic.loadSpawnerData(tag);
            mimic.moveTo(context.pos.getX() + 0.5D, context.pos.getY(), context.pos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            mimic.setLastHurtByMob(event.getPlayer());
            mimic.setPersistenceRequired();
            if (!context.deferredSpirits.isEmpty()) {
                mimic.getPersistentData().put(DEFERRED_SPIRITS_KEY, context.deferredSpirits.copy());
            }

            if (level.addFreshEntity(mimic)) {
                event.setExpToDrop(0);
            } else {
                releaseSpiritTags(level, context.pos.getX() + 0.5D, context.pos.getY(),
                        context.pos.getZ() + 0.5D, context.deferredSpirits);
            }
        } finally {
            BREAK_CONTEXT.remove();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mimicDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof SpawnerMimicEntity mimic)
                || !(mimic.level() instanceof ServerLevel level)) return;

        dropMimicRewards(mimic, level);

        CompoundTag persistent = mimic.getPersistentData();
        if (persistent.contains(DEFERRED_SPIRITS_KEY, Tag.TAG_LIST)) {
            ListTag spirits = persistent.getList(DEFERRED_SPIRITS_KEY, Tag.TAG_COMPOUND).copy();
            persistent.remove(DEFERRED_SPIRITS_KEY);
            releaseSpiritTags(level, mimic.getX(), mimic.getY(), mimic.getZ(), spirits);
        }
    }

    private static void makeMimicTwiceAsFast(CompoundTag tag) {
        if (tag.contains("MinSpawnDelay", Tag.TAG_ANY_NUMERIC)) {
            tag.putShort("MinSpawnDelay", (short)((int)(tag.getShort("MinSpawnDelay") / 2.0D)));
        }
        if (tag.contains("MaxSpawnDelay", Tag.TAG_ANY_NUMERIC)) {
            tag.putShort("MaxSpawnDelay", (short)((int)(tag.getShort("MaxSpawnDelay") / 2.0D)));
        }
        if (tag.contains("Delay", Tag.TAG_ANY_NUMERIC)) {
            tag.putShort("Delay", (short)20);
        }
    }

    private static void dropMimicRewards(SpawnerMimicEntity mimic, ServerLevel level) {
        mimic.spawnAtLocation(new ItemStack(Items.EMERALD, 4 + level.random.nextInt(13)));
        mimic.spawnAtLocation(new ItemStack(Items.DIAMOND, 2 + level.random.nextInt(7)));
        mimic.spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE, 1 + level.random.nextInt(2)));

        if (level.random.nextFloat() >= 0.25F) return;
        CompoundTag spawnData = ((SpawnerMimicAccessor)(Object)mimic).cursedSpawners$getSpawnData();
        CompoundTag entityTag = spawnData.contains("entity", Tag.TAG_COMPOUND)
                ? spawnData.getCompound("entity") : spawnData;
        EntityType.by(entityTag).ifPresent(entityType -> {
            SpawnEggItem egg = ForgeSpawnEggItem.fromEntityType(entityType);
            if (egg == null) egg = SpawnEggItem.byId(entityType);
            if (egg != null) mimic.spawnAtLocation(new ItemStack(egg));
        });
    }

    private static boolean isDifficultSpawnerSpirit(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null
                && DIFFICULT_SPAWNERS_NAMESPACE.equals(id.getNamespace())
                && id.getPath().contains("spirit");
    }

    private static void releaseSpiritTags(ServerLevel level, double x, double y, double z, ListTag spirits) {
        for (int i = 0; i < spirits.size(); ++i) {
            CompoundTag spiritTag = spirits.getCompound(i).copy();
            Entity spirit = EntityType.loadEntityRecursive(spiritTag, level, loaded -> {
                double offsetX = (level.random.nextDouble() - 0.5D) * 1.5D;
                double offsetZ = (level.random.nextDouble() - 0.5D) * 1.5D;
                loaded.moveTo(x + offsetX, y, z + offsetZ, level.random.nextFloat() * 360.0F, 0.0F);
                return loaded;
            });
            if (spirit != null) level.tryAddFreshEntityWithPassengers(spirit);
        }
    }

    private static final class BreakContext {
        private final ServerLevel level;
        private final BlockPos pos;
        private final boolean finalBreak;
        private final boolean createMimic;
        private final boolean deferSpirits;
        private final CompoundTag spawnerTag;
        private final ListTag deferredSpirits = new ListTag();

        private BreakContext(ServerLevel level, BlockPos pos, boolean finalBreak,
                             boolean createMimic, boolean deferSpirits, CompoundTag spawnerTag) {
            this.level = level;
            this.pos = pos.immutable();
            this.finalBreak = finalBreak;
            this.createMimic = createMimic;
            this.deferSpirits = deferSpirits;
            this.spawnerTag = spawnerTag;
        }

        private static BreakContext reforge(ServerLevel level, BlockPos pos) {
            return new BreakContext(level, pos, false, false, false, null);
        }

        private static BreakContext finalBreak(ServerLevel level, BlockPos pos,
                                               boolean createMimic, CompoundTag spawnerTag) {
            return new BreakContext(level, pos, true, createMimic, createMimic, spawnerTag);
        }

        private boolean suppressesSpirits() {
            return !finalBreak || deferSpirits;
        }
    }
}
