package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.model.OwlModel;
import com.harry.wildcraft.entity.OwlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OwlRenderer extends GeoEntityRenderer<OwlEntity> {
    public OwlRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new OwlModel());
    }

    @Override
    public ResourceLocation getTextureLocation(OwlEntity e) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/owl.png");
    }
}