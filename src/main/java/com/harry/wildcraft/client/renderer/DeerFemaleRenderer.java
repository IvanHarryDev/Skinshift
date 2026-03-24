package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.client.model.DeerModel;
import com.harry.wildcraft.entity.DeerFemaleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DeerFemaleRenderer extends GeoEntityRenderer<DeerFemaleEntity> {
    public DeerFemaleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DeerModel<>());
    }

    @Override
    public ResourceLocation getTextureLocation(DeerFemaleEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/deer_female.png");
    }
}
