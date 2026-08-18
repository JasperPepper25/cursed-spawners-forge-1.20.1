package com.provismet.cursedspawners.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

abstract class ExplanationParticle extends TextureSheetParticle {
    protected final SpriteSet sprites;

    protected ExplanationParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0, 0.025D, 0);
        this.sprites = sprites;
        this.lifetime = 20;
        this.quadSize = 0.35F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            this.setSpriteFromAge(sprites);
            this.alpha = Math.max(0.0F, 1.0F - (float)age / (float)lifetime);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
