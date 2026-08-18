/* MODIFIED unofficial Forge 1.20.1 renderer backport; see NOTICE and LICENSE. */
package com.provismet.cursedspawners.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.provismet.cursedspawners.CursedSpawners;
import com.provismet.cursedspawners.client.ClientEvents;
import com.provismet.cursedspawners.client.model.SpawnerMimicModel;
import com.provismet.cursedspawners.entity.SpawnerMimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class SpawnerMimicRenderer extends MobRenderer<SpawnerMimicEntity, SpawnerMimicModel> {
    private static final ResourceLocation TEXTURE = CursedSpawners.id("textures/entity/spawner_mimic.png");
    private final EntityRenderDispatcher dispatcher;

    public SpawnerMimicRenderer(EntityRendererProvider.Context context) {
        super(context, new SpawnerMimicModel(context.bakeLayer(ClientEvents.SPAWNER_MIMIC_LAYER)), 0.6F);
        this.dispatcher = context.getEntityRenderDispatcher();
    }

    @Override
    public void render(SpawnerMimicEntity mimic, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        // The original renderer applies no whole-entity spawn scale; the
        // appearance motion is entirely model-keyframe driven.
        super.render(mimic, entityYaw, partialTick, poseStack, buffers, packedLight);

        Entity innerEntity = mimic.getRenderedEntity();
        if (innerEntity == null) return;
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);
        float scale = 0.53125F;
        float maxDimension = Math.max(innerEntity.getBbWidth(), innerEntity.getBbHeight());
        if (maxDimension > 1.0F) scale /= maxDimension;
        poseStack.translate(0.0D, 0.4D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(mimic.getMobRotation(partialTick) * 10.0F));
        poseStack.translate(0.0D, -0.2D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        poseStack.scale(scale, scale, scale);
        this.dispatcher.render(innerEntity, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SpawnerMimicEntity entity) {
        return TEXTURE;
    }
}
