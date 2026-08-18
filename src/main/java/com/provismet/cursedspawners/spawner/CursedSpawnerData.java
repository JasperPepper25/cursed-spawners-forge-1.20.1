package com.provismet.cursedspawners.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface CursedSpawnerData {
    double PASSTHROUGH_MIMIC_CHANCE = -1.0D;

    void cursedSpawners$tick(ServerLevel level, BlockPos pos);
    boolean cursedSpawners$attemptBreak(ServerLevel level, BlockPos pos);
    double cursedSpawners$getMimicChance();
    void cursedSpawners$dropRewardLoot(ServerLevel level, BlockPos pos);
}
