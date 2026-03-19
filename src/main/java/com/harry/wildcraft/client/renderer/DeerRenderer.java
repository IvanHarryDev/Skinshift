package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.model.DeerModel;
import com.harry.wildcraft.entity.DeerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DeerRenderer extends GeoEntityRenderer<DeerEntity> {
    public DeerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DeerModel());
    }

    @Override
    public ResourceLocation getTextureLocation(DeerEntity e) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/deer.png");
    }
}