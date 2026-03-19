package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SkinwalkerModel extends GeoModel<SkinwalkerEntity> {
    @Override
    public ResourceLocation getModelResource(SkinwalkerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "geo/skinwalker.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SkinwalkerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/skinwalker.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SkinwalkerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "animations/skinwalker.animation.json");
    }
}