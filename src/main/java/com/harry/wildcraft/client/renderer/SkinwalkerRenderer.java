package com.harry.wildcraft.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import com.harry.wildcraft.client.model.SkinwalkerModel;
import com.harry.wildcraft.WildCraftMod;

public class SkinwalkerRenderer extends GeoEntityRenderer<SkinwalkerEntity> {
    public SkinwalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SkinwalkerModel());
    }

    @Override
    public ResourceLocation getTextureLocation(SkinwalkerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/skinwalker.png");
    }
}