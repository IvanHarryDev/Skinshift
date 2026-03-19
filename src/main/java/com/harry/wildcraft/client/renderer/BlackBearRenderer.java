package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.model.BlackBearModel;
import com.harry.wildcraft.entity.BlackBearEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BlackBearRenderer extends GeoEntityRenderer<BlackBearEntity> {
    public BlackBearRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BlackBearModel());
    }

    @Override
    public ResourceLocation getTextureLocation(BlackBearEntity e) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/black_bear.png");
    }
}