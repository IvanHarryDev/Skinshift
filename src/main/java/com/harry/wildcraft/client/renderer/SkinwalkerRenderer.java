package com.harry.wildcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.client.model.SkinwalkerModel;

public class SkinwalkerRenderer extends GeoEntityRenderer<SkinwalkerEntity> {

    public SkinwalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SkinwalkerModel());
    }

    @Override
    public ResourceLocation getTextureLocation(SkinwalkerEntity entity) {
        return this.model.getTextureResource(entity);
    }

    @Override
    public void render(SkinwalkerEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isMorphed() && !entity.isMorphing()) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}