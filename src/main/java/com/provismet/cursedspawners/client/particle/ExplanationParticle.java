package com.provismet.cursedspawners.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Forge 1.20.1 translation of the original AbstractExplanationParticle. */
abstract class ExplanationParticle extends TextureSheetParticle {
    protected final SpriteSet sprites;

    protected ExplanationParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = 0.0D;
        this.yd = 0.1D;
        this.zd = 0.0D;
        this.lifetime = 40;
        this.friction = 0.75F;
        this.quadSize = 0.25F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
