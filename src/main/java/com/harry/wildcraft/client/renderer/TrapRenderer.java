package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.block.TrapBlockEntity;
import com.harry.wildcraft.client.model.TrapModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TrapRenderer extends GeoBlockRenderer<TrapBlockEntity> {
    public TrapRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new TrapModel());
    }
    @Override
    public ResourceLocation getTextureLocation(TrapBlockEntity block) {
        return ResourceLocation.fromNamespaceAndPath(WildCraftMod.MOD_ID, "textures/block/trap_block.png");
    }
}