package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.block.TipiBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TipiModel extends GeoModel<TipiBlockEntity> {

    @Override
    public ResourceLocation getModelResource(TipiBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID,"geo/block/tipi.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TipiBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID,"textures/block/tipi.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TipiBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/block/tipi.animation.json");
    }
}