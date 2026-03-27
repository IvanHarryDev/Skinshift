package com.harry.wildcraft.client.model;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.block.TrapBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TrapModel extends GeoModel<TrapBlockEntity> {
    @Override
    public ResourceLocation getModelResource(TrapBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "geo/block/trap_block.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(TrapBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "textures/block/trap_block.png");
    }
    @Override
    public ResourceLocation getAnimationResource(TrapBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(
                WildCraftMod.MOD_ID, "animations/block/trap_block.animation.json");
    }
}