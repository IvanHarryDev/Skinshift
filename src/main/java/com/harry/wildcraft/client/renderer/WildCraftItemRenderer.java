package com.harry.wildcraft.client.renderer;

import com.harry.wildcraft.WildCraftMod;
import com.harry.wildcraft.block.TipiBlockEntity;
import com.harry.wildcraft.block.TrapBlockEntity;
import com.harry.wildcraft.client.model.TipiModel;
import com.harry.wildcraft.client.model.TrapModel;
import com.harry.wildcraft.init.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WildCraftItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static WildCraftItemRenderer instance;

    private TipiBlockEntity dummyTipi;
    private TrapBlockEntity dummyTrap;

    private GeoBlockRenderer<TipiBlockEntity> tipiRenderer;
    private GeoBlockRenderer<TrapBlockEntity> trapRenderer;

    private boolean initialized = false;

    public WildCraftItemRenderer() {
        super(null, null);
    }

    public static WildCraftItemRenderer getInstance() {
        if (instance == null) {
            instance = new WildCraftItemRenderer();
        }
        return instance;
    }

    private void ensureInitialized() {
        if (initialized) return;
        initialized = true;

        BlockState tipiState = ModBlocks.TIPI.get().defaultBlockState();
        dummyTipi = new TipiBlockEntity(BlockPos.ZERO, tipiState);

        BlockState trapState = ModBlocks.TRAP_BLOCK.get().defaultBlockState();
        dummyTrap = new TrapBlockEntity(BlockPos.ZERO, trapState);

        tipiRenderer = new GeoBlockRenderer<TipiBlockEntity>(new TipiModel()) {
            @Override
            public ResourceLocation getTextureLocation(TipiBlockEntity block) {
                return ResourceLocation.fromNamespaceAndPath(
                        WildCraftMod.MOD_ID, "textures/block/tipi.png");
            }
        };

        trapRenderer = new GeoBlockRenderer<TrapBlockEntity>(new TrapModel()) {
            @Override
            public ResourceLocation getTextureLocation(TrapBlockEntity block) {
                return ResourceLocation.fromNamespaceAndPath(
                        WildCraftMod.MOD_ID, "textures/block/trap_block.png");
            }
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx,
                             PoseStack pose, MultiBufferSource buffer,
                             int light, int overlay) {
        ensureInitialized();

        if (stack.getItem() == ModBlocks.TIPI.get().asItem()) {
            renderTipi(ctx, pose, buffer, light, overlay);
        } else if (stack.getItem() instanceof com.harry.wildcraft.item.TrapItem) {
            renderTrap(ctx, pose, buffer, light, overlay);
        }
    }

    // TRAP
    private void renderTrap(ItemDisplayContext ctx, PoseStack pose,
                            MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();

        switch (ctx) {
            case GUI -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(40));
                pose.mulPose(Axis.YP.rotationDegrees(21));
                pose.translate(0.0, 4.25 / 16.0, 0.0);
                pose.scale(0.8f, 0.8f, 0.8f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(43));
                pose.translate(0.0, 5.0 / 16.0, 0.5 / 16.0);
                pose.scale(0.8f, 0.8f, 0.8f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(43));
                pose.translate(0.0, 5.0 / 16.0, 0.5 / 16.0);
                pose.scale(0.8f, 0.8f, 0.8f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(40));
                pose.mulPose(Axis.YP.rotationDegrees(21));
                pose.translate(0.0, 4.25 / 16.0, 0.0);
                pose.scale(0.8f, 0.8f, 0.8f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(40));
                pose.mulPose(Axis.YP.rotationDegrees(-21));
                pose.translate(0.0, 4.25 / 16.0, 0.0);
                pose.scale(0.8f, 0.8f, 0.8f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case GROUND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.translate(2.75 / 16.0, 4.0 / 16.0, 0.0);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case HEAD -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.translate(0.0, 4.0 / 16.0, 0.0);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case FIXED -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.translate(0.0, -0.05, 0.45);
                pose.scale(0.9f, 0.9f, 0.9f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            default -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.translate(0.0, 4.0 / 16.0, 0.0);
                pose.scale(0.8f, 0.8f, 0.8f);
                pose.translate(-0.5, -0.5, -0.5);
            }
        }

        trapRenderer.defaultRender(pose, dummyTrap, buffer,
                null, null, 0, light, overlay);

        pose.popPose();
    }

    // TIPI
    private void renderTipi(ItemDisplayContext ctx, PoseStack pose,
                            MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();

        switch (ctx) {
            case GUI -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.XP.rotationDegrees(20));
                pose.mulPose(Axis.YP.rotationDegrees(40));
                pose.translate(0.0, -0.22, 0.0);
                pose.scale(0.2f, 0.2f, 0.2f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.translate(0.0, -0.05, 0.0);
                pose.scale(0.22f, 0.22f, 0.22f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.translate(0.0, -0.05, 0.0);
                pose.scale(0.22f, 0.22f, 0.22f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(160));
                pose.translate(0.0, -0.05, 0.1);
                pose.scale(0.2f, 0.2f, 0.2f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(-160));
                pose.translate(0.0, -0.05, 0.1);
                pose.scale(0.2f, 0.2f, 0.2f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            case GROUND -> {
                pose.translate(0.5, 0.25, 0.5);
                pose.scale(0.15f, 0.15f, 0.15f);
                pose.translate(-0.5, 0.0, -0.5);
            }
            case FIXED -> {
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(45));
                pose.translate(0.0, -0.15, 0.0);
                pose.scale(0.18f, 0.18f, 0.18f);
                pose.translate(-0.5, -0.5, -0.5);
            }
            default -> {
                pose.translate(0.5, 0.25, 0.5);
                pose.scale(0.18f, 0.18f, 0.18f);
                pose.translate(-0.5, 0.0, -0.5);
            }
        }

        tipiRenderer.defaultRender(pose, dummyTipi, buffer,
                null, null, 0, light, overlay);

        pose.popPose();
    }
}