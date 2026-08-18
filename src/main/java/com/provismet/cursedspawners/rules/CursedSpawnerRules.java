/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.rules;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.provismet.cursedspawners.network.CursedNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Fabric API supplied double gamerules in the 1.21 original. Vanilla/Forge 1.20.1 only exposes
 * built-in boolean/integer GameRules, so this class preserves the same command surface and
 * per-world persistence with SavedData.
 */
public final class CursedSpawnerRules extends SavedData {
    private static final String DATA_NAME = "cursed_spawners_gamerules";

    public static final String MIMIC_CHANCE = "spawnerMimicChance";
    public static final String MINING_SPEED = "spawnerMiningSpeedModifier";
    public static final String ACTION_CHANCE = "spawnerActionChance";

    private double mimicChance = 0.1D;
    private double miningSpeedModifier = 0.5D;
    private double actionChance = 0.333D;

    public CursedSpawnerRules() {}

    public static CursedSpawnerRules load(CompoundTag tag) {
        CursedSpawnerRules data = new CursedSpawnerRules();
        if (tag.contains(MIMIC_CHANCE, 99)) data.mimicChance = clamp01(tag.getDouble(MIMIC_CHANCE));
        if (tag.contains(MINING_SPEED, 99)) data.miningSpeedModifier = Math.max(0.0D, tag.getDouble(MINING_SPEED));
        if (tag.contains(ACTION_CHANCE, 99)) data.actionChance = clamp01(tag.getDouble(ACTION_CHANCE));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putDouble(MIMIC_CHANCE, mimicChance);
        tag.putDouble(MINING_SPEED, miningSpeedModifier);
        tag.putDouble(ACTION_CHANCE, actionChance);
        return tag;
    }

    public static CursedSpawnerRules get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(CursedSpawnerRules::load, CursedSpawnerRules::new, DATA_NAME);
    }

    public static CursedSpawnerRules get(MinecraftServer server) {
        return get(server.overworld());
    }

    public double mimicChance() { return mimicChance; }
    public double miningSpeedModifier() { return miningSpeedModifier; }
    public double actionChance() { return actionChance; }

    private void setMimicChance(double value) {
        mimicChance = clamp01(value);
        setDirty();
    }

    private void setMiningSpeedModifier(double value, MinecraftServer server) {
        miningSpeedModifier = Math.max(0.0D, value);
        setDirty();
        CursedNetwork.syncAll(server);
    }

    private void setActionChance(double value) {
        actionChance = clamp01(value);
        setDirty();
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("gamerule").requires(source -> source.hasPermission(2));
        root.then(doubleRule(MIMIC_CHANCE, 0.0D, 1.0D,
                CursedSpawnerRules::mimicChance,
                (rules, value, source) -> rules.setMimicChance(value)));
        root.then(doubleRule(MINING_SPEED, 0.0D, Double.MAX_VALUE,
                CursedSpawnerRules::miningSpeedModifier,
                (rules, value, source) -> rules.setMiningSpeedModifier(value, source.getServer())));
        root.then(doubleRule(ACTION_CHANCE, 0.0D, 1.0D,
                CursedSpawnerRules::actionChance,
                (rules, value, source) -> rules.setActionChance(value)));
        // Brigadier merges a duplicate literal node into vanilla's existing /gamerule root.
        event.getDispatcher().register(root);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> doubleRule(
            String name,
            double min,
            double max,
            Getter getter,
            Setter setter) {
        return Commands.literal(name)
                .executes(ctx -> {
                    CursedSpawnerRules rules = get(ctx.getSource().getServer());
                    double value = getter.get(rules);
                    ctx.getSource().sendSuccess(() -> Component.literal(name + " = " + value), false);
                    return (int)Math.round(value * 1000.0D);
                })
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(min, max))
                        .executes(ctx -> {
                            double value = DoubleArgumentType.getDouble(ctx, "value");
                            CursedSpawnerRules rules = get(ctx.getSource().getServer());
                            setter.set(rules, value, ctx.getSource());
                            ctx.getSource().sendSuccess(() -> Component.literal(name + " = " + getter.get(rules)), true);
                            return (int)Math.round(getter.get(rules) * 1000.0D);
                        }));
    }

    @FunctionalInterface private interface Getter { double get(CursedSpawnerRules rules); }
    @FunctionalInterface private interface Setter { void set(CursedSpawnerRules rules, double value, CommandSourceStack source); }
}
