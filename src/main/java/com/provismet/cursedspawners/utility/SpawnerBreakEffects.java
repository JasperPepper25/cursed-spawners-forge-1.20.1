/* MODIFIED from Cursed Spawners by Provismet; unofficial Forge 1.20.1 backport. */
package com.provismet.cursedspawners.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SpawnerBreakEffects {
    private SpawnerBreakEffects() {}

    public static final String NORMAL_BREAK = "normal";
    public static final String SUMMON_VEX = "vex";
    public static final String SUMMON_SILVERFISH = "silverfish";
    public static final String CURSE = "curse";
    private static final List<String> RANDOM = List.of(SUMMON_VEX, SUMMON_SILVERFISH, CURSE);

    public static String random(ServerLevel level) {
        return RANDOM.get(level.random.nextInt(RANDOM.size()));
    }

    public static void execute(String effect, ServerLevel level, BlockPos pos) {
        switch (effect) {
            case SUMMON_VEX -> summonVex(level, pos);
            case SUMMON_SILVERFISH -> summonSilverfish(level, pos);
            case CURSE -> curse(level, pos);
            default -> { /* normal break has no additional action */ }
        }
    }

    private static void summonVex(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < 3; ++i) {
            Vex vex = EntityType.VEX.create(level);
            if (vex == null) continue;
            vex.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
            vex.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            level.addFreshEntity(vex);
        }
    }

    private static void summonSilverfish(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < 5; ++i) {
            Silverfish silverfish = EntityType.SILVERFISH.create(level);
            if (silverfish == null) continue;
            silverfish.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
            level.addFreshEntity(silverfish);
        }
    }

    private static void curse(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB search = AABB.ofSize(center, 16.0D, 16.0D, 16.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, search, p -> {
            if (p.isCreative() || p.isSpectator()) return false;
            Vec3 delta = p.position().subtract(center);
            return delta.x * delta.x + delta.z * delta.z <= 64.0D && Math.abs(delta.y) <= 8.0D;
        })) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
        }
    }
}
