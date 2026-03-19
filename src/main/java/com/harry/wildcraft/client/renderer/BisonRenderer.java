package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.model.BisonModel;
import com.harry.wildcraft.entity.BisonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BisonRenderer extends GeoEntityRenderer<BisonEntity> {
    public BisonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BisonModel());
    }

    @Override
    public ResourceLocation getTextureLocation(BisonEntity e) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/bison.png");
    }
}