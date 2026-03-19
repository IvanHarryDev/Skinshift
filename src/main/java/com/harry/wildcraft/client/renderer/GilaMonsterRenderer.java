package com.harry.wildcraft.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.harry.wildcraft.entity.GilaMonsterEntity;
import com.harry.wildcraft.client.model.GilaMonsterModel;
import com.harry.wildcraft.WildCraftMod;

public class GilaMonsterRenderer extends GeoEntityRenderer<GilaMonsterEntity> {
    public GilaMonsterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GilaMonsterModel());
    }

    @Override
    public ResourceLocation getTextureLocation(GilaMonsterEntity e) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/gila_monster.png");
    }
}