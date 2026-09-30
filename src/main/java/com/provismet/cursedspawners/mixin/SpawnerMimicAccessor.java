package com.provismet.cursedspawners.mixin;

import com.provismet.cursedspawners.entity.SpawnerMimicEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpawnerMimicEntity.class)
public interface SpawnerMimicAccessor {
    @Accessor(value = "spawnData", remap = false)
    CompoundTag cursedSpawners$getSpawnData();
}
