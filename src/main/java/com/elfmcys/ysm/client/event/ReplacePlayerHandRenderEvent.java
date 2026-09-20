package com.elfmcys.ysm.client.event;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.capability.PlayerAnimatableCapabilityProvider;
import com.elfmcys.ysm.model.resource.client.ModelRenderTarget;
import com.elfmcys.ysm.client.renderer.CustomFirstPersonArmRenderer;
import com.elfmcys.ysm.config.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ReplacePlayerHandRenderEvent {
//    TODO
//    @SubscribeEvent
//    public static void onRenderHand(RenderArmEvent event) {
//        if (!YesSteveModel.isAvailable()) {
//            return;
//        }
//        if (ClientConfig.DISABLE_SELF_MODEL.get()) {
//            return;
//        }
//        if (ClientConfig.DISABLE_SELF_HANDS.get()) {
//            return;
//        }
//        if (!(event.getPlayer() instanceof LocalPlayer player)) {
//            return;
//        }
//
//        player.getCapability(PlayerAnimatableCapabilityProvider.CAP).ifPresent(cap -> {
//            if (!cap.isInitializedAndEnabled()) {
//                return;
//            }
//            HumanoidArm arm = event.getArm();
//            ModelRenderTarget model = cap.getModelRenderTarget();
//            var variant = cap.getModelVariant();
//            if (model == null || variant == null) {
//                return;
//            }
//            PoseStack poseStack = event.getPoseStack();
//            MultiBufferSource multiBufferSource = event.getMultiBufferSource();
//            float partialTick = Minecraft.getInstance().getPartialTick();
//            CustomFirstPersonArmRenderer armRenderer = RegisterEntityRenderersEvent.getFirstPersonArmRenderer();
//            armRenderer.render(player, cap, arm, poseStack, multiBufferSource, event.getPackedLight(), partialTick);
//            event.setCanceled(true);
//        });
//    }
}
