package com.elfmcys.ysm.client.renderer;

import com.elfmcys.ysm.capability.PlayerAnimatableCapability;
import com.elfmcys.ysm.client.entity.CustomFirstPersonArmEntity;
import com.elfmcys.ysm.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.ysm.geckolib3.core.util.Color;
import com.elfmcys.ysm.geckolib3.geo.CustomTranslucentRenderType;
import com.elfmcys.ysm.natives.render.NativeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.common.MinecraftForge;

public class CustomFirstPersonArmRenderer {
    private CustomFirstPersonArmEntity armEntity = null;

    @SuppressWarnings("all")
    public void render(LocalPlayer player, PlayerAnimatableCapability cap, HumanoidArm arm,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, float partialTick) {
        if (armEntity == null || armEntity.getEntity() != player) {
            armEntity = new CustomFirstPersonArmEntity(player, cap);
        }

        armEntity.checkModelUpdate();
        var data = armEntity.update(partialTick);
        if (data == null || !data.modelState.isValid()) {
            return;
        }

        var renderEvent = new SpecialPlayerRenderEvent(player, cap, cap.getModelId());
        if (MinecraftForge.EVENT_BUS.post(renderEvent)) {
            return;
        }

        var textureLocation = renderEvent.getTextureLocationOverride() == null ? cap.getTextureLocation() : renderEvent.getTextureLocationOverride();
        var vertexConsumer = bufferSource.getBuffer(CustomTranslucentRenderType.create(textureLocation));

        poseStack.pushPose();
        if (arm == HumanoidArm.LEFT) {
            poseStack.translate(0.25, 1.8, 0);
        } else {
            poseStack.translate(-0.25, 1.8, 0);
        }
        poseStack.scale(-1, -1, 1);

        NativeRenderer.render(vertexConsumer, poseStack.last(), data.modelState.getNativeState(), data.modelState.getVertexCount(),
                packedLight, OverlayTexture.NO_OVERLAY, Color.WHITE.getColor(), data.ctx.nativeType());
        poseStack.popPose();
    }
}
