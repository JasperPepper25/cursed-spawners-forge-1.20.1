/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.client.particle;

import com.provismet.cursedspawners.particle.AOEChargingParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public final class AOEChargingParticle extends TextureSheetParticle {
    private AOEChargingParticle(ClientLevel level, double x, double y, double z,
                                AOEChargingParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.lifetime = Math.max(1, options.duration());
        this.quadSize = 0.8F;
        this.hasPhysics = false;
        int color = options.color();
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) this.alpha = Math.max(0.0F, 1.0F - (float)age / (float)lifetime);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<AOEChargingParticleOptions> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(AOEChargingParticleOptions options, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new AOEChargingParticle(level, x, y, z, options, sprites);
        }
    }
}
