/* MODIFIED unofficial Forge 1.20.1 backport; replaces the 1.21 MapCodec/PacketCodec particle payload. */
package com.provismet.cursedspawners.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.provismet.cursedspawners.registry.ModRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

public record AOEChargingParticleOptions(int duration, int color) implements ParticleOptions {
    public static final Codec<AOEChargingParticleOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("duration").forGetter(AOEChargingParticleOptions::duration),
            Codec.INT.fieldOf("color").forGetter(AOEChargingParticleOptions::color)
    ).apply(instance, AOEChargingParticleOptions::new));

    public static final ParticleOptions.Deserializer<AOEChargingParticleOptions> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public AOEChargingParticleOptions fromCommand(ParticleType<AOEChargingParticleOptions> type,
                                                               StringReader reader) throws CommandSyntaxException {
                    reader.expect(' ');
                    int duration = reader.readInt();
                    reader.expect(' ');
                    int color = reader.readInt();
                    return new AOEChargingParticleOptions(duration, color);
                }

                @Override
                public AOEChargingParticleOptions fromNetwork(ParticleType<AOEChargingParticleOptions> type,
                                                               FriendlyByteBuf buf) {
                    return new AOEChargingParticleOptions(buf.readVarInt(), buf.readVarInt());
                }
            };

    @Override
    public ParticleType<?> getType() {
        return ModRegistry.AOE_CHARGING.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(duration);
        buf.writeVarInt(color);
    }

    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()) + " " + duration + " " + color;
    }
}
