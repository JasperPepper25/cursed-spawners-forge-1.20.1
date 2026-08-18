/* MODIFIED unofficial Forge 1.20.1 backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.provismet.cursedspawners.particle.AOEChargingParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Forge 1.20.1 translation of LilyLib's FlatParticle + the original
 * Cursed Spawners AOEChargingParticle behavior.
 *
 * The first backport build used a normal camera-facing billboard particle.
 * This implementation deliberately renders the quad horizontally in world
 * space, matching LilyLib's -90 degree X rotation, and keeps the particle
 * stationary at the original blockY + 0.025 spawn position.
 */
public final class AOEChargingParticle extends TextureSheetParticle {
    private float previousSize;
    private final float maximumSize;

    private AOEChargingParticle(ClientLevel level, double x, double y, double z,
                                AOEChargingParticleOptions options, SpriteSet sprites) {
        // Use the no-velocity constructor. Particle's velocity constructor adds
        // randomized motion even when passed zeroes, which caused the indicator
        // to drift upward in the first backport build.
        super(level, x, y, z);
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.hasPhysics = false;

        this.lifetime = Math.max(1, options.duration());
        this.quadSize = 3.0F;
        this.maximumSize = this.quadSize;
        this.previousSize = this.quadSize;
        this.alpha = 0.0F;

        int color = options.color();
        this.rCol = ((color >> 16) & 0xFF) / 255.0F;
        this.gCol = ((color >> 8) & 0xFF) / 255.0F;
        this.bCol = (color & 0xFF) / 255.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        this.previousSize = this.quadSize;
        this.quadSize = this.maximumSize * (1.0F - (float)this.age / (float)this.lifetime);

        // LilyLib's FlatParticle begins fading during the latter half of life;
        // the Cursed Spawners subclass then eases alpha upward by 0.1 each tick.
        if (this.age > this.lifetime / 2) {
            this.alpha = 1.0F - ((float)this.age - (float)(this.lifetime / 2)) / (float)this.lifetime;
        }
        if (this.alpha < 1.0F) this.alpha += 0.1F;
        if (this.alpha > 1.0F) this.alpha = 1.0F;
    }

    @Override
    public float getQuadSize(float partialTick) {
        return Mth.lerp(partialTick, this.previousSize, this.quadSize);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float)(Mth.lerp((double)partialTick, this.xo, this.x) - cameraPos.x());
        float y = (float)(Mth.lerp((double)partialTick, this.yo, this.y) - cameraPos.y());
        float z = (float)(Mth.lerp((double)partialTick, this.zo, this.z) - cameraPos.z());

        // Vanilla particle quads begin in the XY plane. LilyLib's FlatParticle
        // applies -90 degrees around X, putting the indicator flat on the XZ floor.
        Quaternionf rotation = new Quaternionf().rotateX(-(float)Math.PI / 2.0F);
        Vector3f[] corners = new Vector3f[] {
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F,  1.0F, 0.0F),
                new Vector3f( 1.0F,  1.0F, 0.0F),
                new Vector3f( 1.0F, -1.0F, 0.0F)
        };

        float size = this.getQuadSize(partialTick);
        for (Vector3f corner : corners) {
            corner.rotate(rotation).mul(size).add(x, y, z);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTick);

        buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<AOEChargingParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(AOEChargingParticleOptions options, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new AOEChargingParticle(level, x, y, z, options, sprites);
        }
    }
}
