package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.BisonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BisonModel extends GeoModel<BisonEntity> {
    @Override
    public ResourceLocation getModelResource(BisonEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/bison.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(BisonEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/entity/bison.png");
    }
    @Override
    public ResourceLocation getAnimationResource(BisonEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/bison.animation.json");
    }
}