package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.DeerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DeerModel extends GeoModel<DeerEntity> {
    @Override
    public ResourceLocation getModelResource(DeerEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/deer.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(DeerEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/entity/deer.png");
    }
    @Override
    public ResourceLocation getAnimationResource(DeerEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/deer.animation.json");
    }
}