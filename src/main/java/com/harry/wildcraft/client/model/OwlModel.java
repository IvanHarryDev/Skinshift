package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.OwlEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OwlModel extends GeoModel<OwlEntity> {
    @Override
    public ResourceLocation getModelResource(OwlEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/owl.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(OwlEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/entity/.png");
    }
    @Override
    public ResourceLocation getAnimationResource(OwlEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/owl.animation.json");
    }
}