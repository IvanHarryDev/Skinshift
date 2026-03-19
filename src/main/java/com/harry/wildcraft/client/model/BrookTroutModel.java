package com.harry.wildcraft.client.model;


import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.BrookTroutEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BrookTroutModel extends GeoModel<BrookTroutEntity> {
    @Override
    public ResourceLocation getModelResource(BrookTroutEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/brook_trout.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(BrookTroutEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/entity/brook_trout.png");
    }
    @Override
    public ResourceLocation getAnimationResource(BrookTroutEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/brook_trout.animation.json");
    }
}