package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.skinwalker.SkinwalkerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SkinwalkerModel extends GeoModel<SkinwalkerEntity> {

    private static final ResourceLocation TEXTURE_NORMAL =
            ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/skinwalker.png");
    private static final ResourceLocation TEXTURE_PIG_AND_WOLF =
            ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/entity/skinwalker_pig_and_wolf.png");

    @Override
    public ResourceLocation getModelResource(SkinwalkerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "geo/skinwalker.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SkinwalkerEntity entity) {
        if (entity.isMorphing()) {
            return TEXTURE_PIG_AND_WOLF;
        }
        return TEXTURE_NORMAL;
    }

    @Override
    public ResourceLocation getAnimationResource(SkinwalkerEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "animations/skinwalker.animation.json");
    }
}