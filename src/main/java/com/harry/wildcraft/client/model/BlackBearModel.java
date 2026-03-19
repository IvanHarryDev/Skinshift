package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.BlackBearEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BlackBearModel extends GeoModel<BlackBearEntity> {
    @Override
    public ResourceLocation getModelResource(BlackBearEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/black_bear.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(BlackBearEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/entity/black_bear.png");
    }
    @Override
    public ResourceLocation getAnimationResource(BlackBearEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/black_bear.animation.json");
    }
}
