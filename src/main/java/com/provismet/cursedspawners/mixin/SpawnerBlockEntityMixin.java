/*
 * MODIFIED from Cursed Spawners by Provismet (tag 1.0.0-mc1.21).
 * Unofficial Forge 1.20.1 backport; see NOTICE and LICENSE.
 */
package com.provismet.cursedspawners.mixin;

import com.provismet.cursedspawners.particle.AOEChargingParticleOptions;
import com.provismet.cursedspawners.registry.ModRegistry;
import com.provismet.cursedspawners.rules.CursedSpawnerRules;
import com.provismet.cursedspawners.spawner.CursedSpawnerData;
import com.provismet.cursedspawners.utility.SpawnerBreakEffects;
import com.provismet.cursedspawners.utility.SpawnerEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerBlockEntityMixin extends BlockEntity implements CursedSpawnerData, Container {
    @Unique private static final String REFORGE_ACTIONS = "ReforgeActions";
    @Unique private static final String BREAK_ACTION = "BreakAction";
    @Unique private static final String SHOULD_GENERATE = "ShouldGenerateEffects";
    @Unique private static final String MIMIC_CHANCE = "MimicChance";

    @Unique private static final String CAN_KNOCKBACK = "CanKnockback";
    @Unique private static final String KNOCKBACK_INTERVAL = "KnockbackInterval";
    @Unique private static final String KNOCKBACK_STRENGTH = "KnockbackStrength";
    @Unique private static final String KNOCKBACK_RADIUS = "KnockbackRadius";
    @Unique private static final String CAN_HEAL = "CanHeal";
    @Unique private static final String HEAL_INTERVAL = "HealInterval";
    @Unique private static final String HEAL_AMOUNT = "HealAmount";
    @Unique private static final String HEAL_RADIUS = "HealRadius";
    @Unique private static final String CAN_BOOST = "CanBoost";
    @Unique private static final String BOOST_INTERVAL = "BoostInterval";
    @Unique private static final String BOOST_RADIUS = "BoostRadius";

    @Unique private final ArrayList<String> cursed$reforgeActions = new ArrayList<>();
    @Unique private String cursed$breakAction = SpawnerBreakEffects.NORMAL_BREAK;
    @Unique private boolean cursed$shouldGenerateEffects = true;
    @Unique private double cursed$mimicChance = PASSTHROUGH_MIMIC_CHANCE;

    @Unique private boolean cursed$canKnockback = false;
    @Unique private int cursed$knockbackTimer = 200;
    @Unique private int cursed$maxKnockbackTimer = 200;
    @Unique private double cursed$knockbackStrength = 0.2D;
    @Unique private double cursed$knockbackRadius = 4.0D;

    @Unique private boolean cursed$canHeal = false;
    @Unique private int cursed$healTimer = 200;
    @Unique private int cursed$maxHealTimer = 200;
    @Unique private float cursed$healAmount = 0.0F;
    @Unique private double cursed$healRadius = 0.0D;

    @Unique private boolean cursed$canBoost = false;
    @Unique private int cursed$boostTimer = 200;
    @Unique private int cursed$maxBoostTimer = 200;
    @Unique private double cursed$boostRadius = 0.0D;

    @Unique private ResourceLocation cursed$lootTable = null;
    @Unique private long cursed$lootTableSeed = 0L;
    @Unique private final NonNullList<ItemStack> cursed$inventory = NonNullList.withSize(27, ItemStack.EMPTY);

    protected SpawnerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void cursed$readExtendedNbt(CompoundTag tag, CallbackInfo ci) {
        cursed$reforgeActions.clear();
        if (tag.contains(REFORGE_ACTIONS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(REFORGE_ACTIONS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); ++i) cursed$reforgeActions.add(list.getString(i));
        }
        cursed$breakAction = tag.contains(BREAK_ACTION, Tag.TAG_STRING)
                ? tag.getString(BREAK_ACTION) : SpawnerBreakEffects.NORMAL_BREAK;
        cursed$shouldGenerateEffects = !tag.contains(SHOULD_GENERATE) || tag.getBoolean(SHOULD_GENERATE);
        cursed$mimicChance = tag.contains(MIMIC_CHANCE, Tag.TAG_DOUBLE)
                ? tag.getDouble(MIMIC_CHANCE) : PASSTHROUGH_MIMIC_CHANCE;

        cursed$canKnockback = tag.contains(CAN_KNOCKBACK) && tag.getBoolean(CAN_KNOCKBACK);
        cursed$maxKnockbackTimer = tag.contains(KNOCKBACK_INTERVAL, Tag.TAG_INT) ? tag.getInt(KNOCKBACK_INTERVAL) : 200;
        cursed$knockbackStrength = tag.contains(KNOCKBACK_STRENGTH, Tag.TAG_DOUBLE) ? tag.getDouble(KNOCKBACK_STRENGTH) : 0.2D;
        cursed$knockbackRadius = tag.contains(KNOCKBACK_RADIUS, Tag.TAG_DOUBLE) ? tag.getDouble(KNOCKBACK_RADIUS) : 4.0D;

        cursed$canHeal = tag.contains(CAN_HEAL) && tag.getBoolean(CAN_HEAL);
        cursed$maxHealTimer = tag.contains(HEAL_INTERVAL, Tag.TAG_INT) ? tag.getInt(HEAL_INTERVAL) : 200;
        cursed$healAmount = tag.contains(HEAL_AMOUNT, Tag.TAG_FLOAT) ? tag.getFloat(HEAL_AMOUNT) : 0.0F;
        cursed$healRadius = tag.contains(HEAL_RADIUS, Tag.TAG_DOUBLE) ? tag.getDouble(HEAL_RADIUS) : 0.0D;

        cursed$canBoost = tag.contains(CAN_BOOST) && tag.getBoolean(CAN_BOOST);
        cursed$maxBoostTimer = tag.contains(BOOST_INTERVAL, Tag.TAG_INT) ? tag.getInt(BOOST_INTERVAL) : 200;
        cursed$boostRadius = tag.contains(BOOST_RADIUS, Tag.TAG_DOUBLE) ? tag.getDouble(BOOST_RADIUS) : 0.0D;

        // The original clamps the live timers after loading their configured intervals.
        cursed$knockbackTimer = Math.min(cursed$knockbackTimer, cursed$maxKnockbackTimer);
        cursed$healTimer = Math.min(cursed$healTimer, cursed$maxHealTimer);
        cursed$boostTimer = Math.min(cursed$boostTimer, cursed$maxBoostTimer);

        cursed$lootTable = tag.contains("LootTable", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("LootTable")) : null;
        cursed$lootTableSeed = tag.getLong("LootTableSeed");
        for (int i = 0; i < cursed$inventory.size(); ++i) cursed$inventory.set(i, ItemStack.EMPTY);
        if (cursed$lootTable == null) ContainerHelper.loadAllItems(tag, cursed$inventory);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void cursed$writeExtendedNbt(CompoundTag tag, CallbackInfo ci) {
        ListTag reforge = new ListTag();
        for (String action : cursed$reforgeActions) reforge.add(StringTag.valueOf(action));
        tag.put(REFORGE_ACTIONS, reforge);
        tag.putString(BREAK_ACTION, cursed$breakAction);
        tag.putBoolean(SHOULD_GENERATE, cursed$shouldGenerateEffects);
        tag.putDouble(MIMIC_CHANCE, cursed$mimicChance);

        tag.putBoolean(CAN_KNOCKBACK, cursed$canKnockback);
        tag.putInt(KNOCKBACK_INTERVAL, cursed$maxKnockbackTimer);
        tag.putDouble(KNOCKBACK_STRENGTH, cursed$knockbackStrength);
        tag.putDouble(KNOCKBACK_RADIUS, cursed$knockbackRadius);

        tag.putBoolean(CAN_HEAL, cursed$canHeal);
        tag.putInt(HEAL_INTERVAL, cursed$maxHealTimer);
        tag.putFloat(HEAL_AMOUNT, cursed$healAmount);
        tag.putDouble(HEAL_RADIUS, cursed$healRadius);

        tag.putBoolean(CAN_BOOST, cursed$canBoost);
        tag.putInt(BOOST_INTERVAL, cursed$maxBoostTimer);
        tag.putDouble(BOOST_RADIUS, cursed$boostRadius);

        if (cursed$lootTable != null) {
            tag.putString("LootTable", cursed$lootTable.toString());
            if (cursed$lootTableSeed != 0L) tag.putLong("LootTableSeed", cursed$lootTableSeed);
        }
        ContainerHelper.saveAllItems(tag, cursed$inventory, false);
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void cursed$serverTick(Level level, BlockPos pos, BlockState state,
                                          SpawnerBlockEntity blockEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel server && blockEntity instanceof CursedSpawnerData data) {
            data.cursedSpawners$tick(server, pos);
        }
    }

    @Override
    public void cursedSpawners$tick(ServerLevel level, BlockPos pos) {
        if (cursed$shouldGenerateEffects && CursedSpawnerRules.get(level).actionChance() > 0.0D) {
            cursed$generateEffects(level);
            setChanged();
        }

        Vec3 center = Vec3.atCenterOf(pos);
        if (cursed$canKnockback) {
            --cursed$knockbackTimer;
            if (cursed$knockbackTimer == cursed$maxKnockbackTimer / 2) {
                level.sendParticles(new AOEChargingParticleOptions(cursed$knockbackTimer, 0xE2E2E2),
                        center.x, pos.getY() + 0.025D, center.z, 1, 0, 0, 0, 0);
            } else if (cursed$knockbackTimer <= 0) {
                cursed$knockbackTimer = cursed$maxKnockbackTimer;
                cursed$performKnockback(level, pos, center);
            }
        }

        if (cursed$canHeal) {
            --cursed$healTimer;
            if (cursed$healTimer == cursed$maxHealTimer / 2) {
                level.sendParticles(new AOEChargingParticleOptions(cursed$healTimer, 0x47BC78),
                        center.x, pos.getY() + 0.025D, center.z, 1, 0, 0, 0, 0);
            } else if (cursed$healTimer <= 0) {
                cursed$healTimer = cursed$maxHealTimer;
                cursed$performHeal(level, pos, center);
            }
        }

        if (cursed$canBoost) {
            --cursed$boostTimer;
            if (cursed$boostTimer == cursed$maxBoostTimer / 2) {
                level.sendParticles(new AOEChargingParticleOptions(cursed$boostTimer, 0xFF8459),
                        center.x, pos.getY() + 0.025D, center.z, 1, 0, 0, 0, 0);
            } else if (cursed$boostTimer <= 0) {
                cursed$boostTimer = cursed$maxBoostTimer;
                cursed$performBoost(level, pos, center);
            }
        }
    }

    @Unique
    private void cursed$generateEffects(ServerLevel level) {
        int dangerLevel = 0;
        double chance = CursedSpawnerRules.get(level).actionChance();

        // Preserve the original RNG consumption order: determine danger level completely first,
        // then perform a separate pass that rolls the concrete effects.
        while (dangerLevel < 7 && level.random.nextDouble() < chance) {
            ++dangerLevel;
        }

        for (int effectIndex = 0; effectIndex < dangerLevel; ++effectIndex) {
            List<SpawnerEffects> possible = new ArrayList<>();
            possible.add(SpawnerEffects.REFORGE);
            if (SpawnerBreakEffects.NORMAL_BREAK.equals(cursed$breakAction)) possible.add(SpawnerEffects.BREAK);
            if (!cursed$canKnockback) possible.add(SpawnerEffects.KNOCKBACK);
            if (!cursed$canHeal) possible.add(SpawnerEffects.HEAL);
            if (!cursed$canBoost) possible.add(SpawnerEffects.BOOST);

            SpawnerEffects selected = possible.get(level.random.nextInt(possible.size()));
            switch (selected) {
                case BREAK -> cursed$breakAction = SpawnerBreakEffects.random(level);
                case REFORGE -> cursed$reforgeActions.add(SpawnerBreakEffects.random(level));
                case KNOCKBACK -> {
                    cursed$canKnockback = true;
                    cursed$maxKnockbackTimer = Mth.nextInt(level.random, 100, 160);
                    cursed$knockbackTimer = Math.min(cursed$knockbackTimer, cursed$maxKnockbackTimer);
                    cursed$knockbackStrength = level.random.triangle(1.5D, 0.5D);
                    cursed$knockbackRadius = level.random.triangle(5.0D, 0.75D);
                }
                case HEAL -> {
                    cursed$canHeal = true;
                    cursed$maxHealTimer = Mth.nextInt(level.random, 80, 160);
                    cursed$healTimer = Math.min(cursed$healTimer, cursed$maxHealTimer);
                    cursed$healAmount = 2.0F;
                    cursed$healRadius = level.random.triangle(5.0D, 1.0D);
                }
                case BOOST -> {
                    cursed$canBoost = true;
                    cursed$maxBoostTimer = Mth.nextInt(level.random, 120, 200);
                    cursed$boostTimer = Math.min(cursed$boostTimer, cursed$maxBoostTimer);
                    cursed$boostRadius = level.random.triangle(8.0D, 4.0D);
                }
            }
        }

        if (dangerLevel > 0 && cursed$lootTable == null) {
            cursed$lootTable = dangerLevel < 3 ? BuiltInLootTables.JUNGLE_TEMPLE
                    : dangerLevel < 6 ? BuiltInLootTables.SIMPLE_DUNGEON
                    : BuiltInLootTables.WOODLAND_MANSION;
        }
        cursed$shouldGenerateEffects = false;
    }

    @Unique
    private void cursed$performKnockback(ServerLevel level, BlockPos pos, Vec3 center) {
        // GUST_EMITTER_SMALL is 1.21-only. One POOF emitter is the closest vanilla 1.20.1 fallback
        // without adding a second custom particle implementation solely for the gust burst.
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        double radius = cursed$knockbackRadius;
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) continue;
            Vec3 p = player.position();
            if (Math.abs(p.y - center.y) > radius || Math.hypot(p.x - center.x, p.z - center.z) > radius) continue;
            double actualStrength = cursed$knockbackStrength * (1.0D - player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (actualStrength <= 0.0D) continue;
            double ySign = player.getY() >= pos.getY() ? 0.5D : -0.5D;
            Vec3 impulse = new Vec3(player.getX() - center.x, ySign, player.getZ() - center.z)
                    .normalize().scale(actualStrength);
            player.push(impulse.x, impulse.y, impulse.z);
            player.hurtMarked = true;
        }
        level.playSound(null, pos, ModRegistry.BLOCK_SPAWNER_KNOCKBACK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Unique
    private void cursed$performHeal(ServerLevel level, BlockPos pos, Vec3 center) {
        // Original: one custom heal indicator at the spawner, then 8 happy-villager particles per healed hostile.
        level.sendParticles(ModRegistry.HEAL.get(), center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        AABB box = AABB.ofSize(center, cursed$healRadius, 3.0D, cursed$healRadius);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, box, Monster::isAlive)) {
            monster.heal(cursed$healAmount);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, monster.getX(), monster.getEyeY(),
                    monster.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.0D);
        }
        level.playSound(null, pos, ModRegistry.BLOCK_SPAWNER_HEAL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Unique
    private void cursed$performBoost(ServerLevel level, BlockPos pos, Vec3 center) {
        // Original: one custom boost indicator at the spawner, then 8 happy-villager particles per boosted hostile.
        level.sendParticles(ModRegistry.BOOST.get(), center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        AABB box = AABB.ofSize(center, cursed$boostRadius, 3.0D, cursed$boostRadius);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, box, Monster::isAlive)) {
            monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, monster.getX(), monster.getEyeY(),
                    monster.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.0D);
        }
        level.playSound(null, pos, ModRegistry.BLOCK_SPAWNER_BOOST.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public boolean cursedSpawners$attemptBreak(ServerLevel level, BlockPos pos) {
        if (cursed$reforgeActions.isEmpty()) {
            SpawnerBreakEffects.execute(cursed$breakAction, level, pos);
            return true;
        }
        String action = cursed$reforgeActions.remove(0);
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 20,
                0.5D, 0.5D, 0.5D, 0.0D);
        SpawnerBreakEffects.execute(action, level, pos);
        setChanged();
        return false;
    }

    @Override
    public double cursedSpawners$getMimicChance() {
        return cursed$mimicChance;
    }

    @Unique
    private void cursed$unpackLoot() {
        if (cursed$lootTable == null || !(this.level instanceof ServerLevel server)) return;
        ResourceLocation tableId = cursed$lootTable;
        cursed$lootTable = null;
        LootTable table = server.getServer().getLootData().getLootTable(tableId);
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
                .create(LootContextParamSets.CHEST);
        // Use vanilla's container-fill path rather than assigning generated stacks
        // sequentially. This preserves LootTableSeed, random slot selection, stack
        // splitting, and Forge global-loot-modifier handling. cursed$lootTable is
        // cleared before fill(), so our Container#setItem implementation cannot
        // recurse back into this method.
        table.fill(this, params, cursed$lootTableSeed);
        setChanged();
    }

    @Override
    public void cursedSpawners$dropRewardLoot(ServerLevel level, BlockPos pos) {
        cursed$unpackLoot();
        Vec3 center = Vec3.atCenterOf(pos);
        for (int i = 0; i < cursed$inventory.size(); ++i) {
            ItemStack stack = cursed$inventory.get(i);
            if (stack.isEmpty()) continue;
            level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack.copy()));
            cursed$inventory.set(i, ItemStack.EMPTY);
        }
    }

    /* Original spawners expose a hidden 27-slot lootable inventory that players cannot open. */
    @Override public int getContainerSize() { return 27; }
    @Override public boolean isEmpty() { cursed$unpackLoot(); return cursed$inventory.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { cursed$unpackLoot(); return cursed$inventory.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { cursed$unpackLoot(); ItemStack out = ContainerHelper.removeItem(cursed$inventory, slot, amount); if (!out.isEmpty()) setChanged(); return out; }
    @Override public ItemStack removeItemNoUpdate(int slot) { cursed$unpackLoot(); return ContainerHelper.takeItem(cursed$inventory, slot); }
    @Override public void setItem(int slot, ItemStack stack) { cursed$unpackLoot(); cursed$inventory.set(slot, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); setChanged(); }
    @Override public boolean stillValid(Player player) { return false; }
    @Override public void clearContent() { cursed$unpackLoot(); for (int i = 0; i < cursed$inventory.size(); ++i) cursed$inventory.set(i, ItemStack.EMPTY); setChanged(); }
}

