package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.block.TipiBlockEntity;
import com.harry.wildcraft.client.model.TipiModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TipiRenderer extends GeoBlockRenderer<TipiBlockEntity> {
    public TipiRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new TipiModel());
    }

    @Override
    public ResourceLocation getTextureLocation(TipiBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/block/tipi.png");
    }
}