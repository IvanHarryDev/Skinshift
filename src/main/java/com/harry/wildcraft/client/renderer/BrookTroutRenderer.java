package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.model.BrookTroutModel;
import com.harry.wildcraft.entity.BrookTroutEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BrookTroutRenderer extends GeoEntityRenderer<BrookTroutEntity> {
    public BrookTroutRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BrookTroutModel());
    }

    @Override
    public ResourceLocation getTextureLocation(BrookTroutEntity e) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/brook_trout.png");
    }
}
