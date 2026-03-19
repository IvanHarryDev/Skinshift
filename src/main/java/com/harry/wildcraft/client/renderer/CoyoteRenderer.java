package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.model.CoyoteModel;
import com.harry.wildcraft.entity.CoyoteEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CoyoteRenderer extends GeoEntityRenderer<CoyoteEntity> {
    public CoyoteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CoyoteModel());
    }

    @Override
    public ResourceLocation getTextureLocation(CoyoteEntity e) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/coyote.png");
    }
}