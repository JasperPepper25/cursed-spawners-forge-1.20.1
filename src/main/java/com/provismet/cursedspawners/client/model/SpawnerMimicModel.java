/* Geometry ported from the original Cursed Spawners model; animation API adapted for Forge 1.20.1. */
package com.provismet.cursedspawners.client.model;

import com.provismet.cursedspawners.client.animation.SpawnerMimicAnimations;
import com.provismet.cursedspawners.entity.SpawnerMimicEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class SpawnerMimicModel extends HierarchicalModel<SpawnerMimicEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart legNW;
    private final ModelPart legSW;
    private final ModelPart legSE;
    private final ModelPart legNE;

    public SpawnerMimicModel(ModelPart bakedRoot) {
        this.root = bakedRoot.getChild("root");
        this.body = this.root.getChild("body");
        this.legNW = this.root.getChild("legNW");
        this.legSW = this.root.getChild("legSW");
        this.legSE = this.root.getChild("legSE");
        this.legNE = this.root.getChild("legNE");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition modelRoot = mesh.getRoot();
        PartDefinition root = modelRoot.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-8.0F, -19.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

        root.addOrReplaceChild("legNW", CubeListBuilder.create()
                .texOffs(46, 16).mirror().addBox(-2.0F, -3.0F, -2.0F, 4.0F, 5.0F, 4.0F).mirror(false)
                .texOffs(48, 40).addBox(-2.0F, 2.0F, -3.0F, 5.0F, 1.0F, 1.0F)
                .texOffs(42, 27).addBox(2.0F, 2.0F, -2.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(56, 33).addBox(2.0F, -3.0F, -3.0F, 1.0F, 5.0F, 1.0F), PartPose.offset(8.0F, -3.0F, -8.0F));
        root.addOrReplaceChild("legSW", CubeListBuilder.create()
                .texOffs(38, 16).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 5.0F, 4.0F)
                .texOffs(48, 46).addBox(-2.0F, 2.0F, 2.0F, 5.0F, 1.0F, 1.0F)
                .texOffs(32, 27).addBox(2.0F, 2.0F, -2.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(60, 30).addBox(2.0F, -3.0F, 2.0F, 1.0F, 5.0F, 1.0F), PartPose.offset(8.0F, -3.0F, 8.0F));
        root.addOrReplaceChild("legSE", CubeListBuilder.create()
                .texOffs(38, 16).mirror().addBox(-2.0F, -3.0F, -2.0F, 4.0F, 5.0F, 4.0F).mirror(false)
                .texOffs(48, 44).addBox(-3.0F, 2.0F, 2.0F, 5.0F, 1.0F, 1.0F)
                .texOffs(48, 28).addBox(-3.0F, 2.0F, -2.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(60, 36).addBox(-3.0F, -3.0F, 2.0F, 1.0F, 5.0F, 1.0F), PartPose.offset(-8.0F, -3.0F, 8.0F));
        root.addOrReplaceChild("legNE", CubeListBuilder.create()
                .texOffs(46, 16).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 5.0F, 4.0F)
                .texOffs(48, 42).addBox(-3.0F, 2.0F, -3.0F, 5.0F, 1.0F, 1.0F)
                .texOffs(48, 35).addBox(-3.0F, 2.0F, -2.0F, 1.0F, 1.0F, 4.0F)
                .texOffs(60, 42).addBox(-3.0F, -3.0F, -3.0F, 1.0F, 5.0F, 1.0F), PartPose.offset(-8.0F, -3.0F, -8.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(SpawnerMimicEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.body.yRot = Mth.clamp(netHeadYaw, -30.0F, 30.0F) * Mth.DEG_TO_RAD;

        // These calls mirror the original 1.21 model: walk first, then the
        // independent idle, attack, and spawn animation states.
        this.animateWalk(SpawnerMimicAnimations.WALK, limbSwing, limbSwingAmount, 3.0F, 50.0F);
        this.animate(entity.idleState, SpawnerMimicAnimations.IDLE, ageInTicks);
        this.animate(entity.attackState, SpawnerMimicAnimations.ATTACK, ageInTicks);
        this.animate(entity.spawnState, SpawnerMimicAnimations.SPAWN, ageInTicks);
    }
}
