package com.harry.wildcraft.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import com.harry.wildcraft.entity.GilaMonsterEntity;
import com.harry.wildcraft.WildCraftMod;

public class GilaMonsterModel extends GeoModel<GilaMonsterEntity> {
    @Override
    public ResourceLocation getModelResource(GilaMonsterEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/gila_monster.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(GilaMonsterEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/entity/gila_monster.png");
    }
    @Override
    public ResourceLocation getAnimationResource(GilaMonsterEntity e) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/gila_monster.animation.json");
    }
}