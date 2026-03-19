package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.entity.CoyoteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CoyoteModel extends GeoModel<CoyoteEntity> {
    @Override
    public ResourceLocation getModelResource(CoyoteEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/coyote.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(CoyoteEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/entity/coyote.png");
    }
    @Override
    public ResourceLocation getAnimationResource(CoyoteEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/coyote.animation.json");
    }
}