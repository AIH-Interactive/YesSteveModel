package com.elfmcys.ysm.client.gui.button;

import com.elfmcys.ysm.client.animation.AnimationRegister;
import com.elfmcys.ysm.client.event.RegisterEntityRenderersEvent;
import com.elfmcys.ysm.client.gui.CustomGuiPlayerEntity;
import com.elfmcys.ysm.client.lang.LanguageManager;
import com.elfmcys.ysm.model.resource.client.AcquireResult;
import com.elfmcys.ysm.model.resource.client.ModelRenderTarget;
import com.elfmcys.ysm.model.service.ClientModelService;
import com.elfmcys.ysm.model.resource.client.ResourceLease;
import com.elfmcys.ysm.model.resource.client.ResourceRequest;
import com.elfmcys.ysm.model.domain.Hash256;
import com.elfmcys.ysm.util.RenderUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CatalogTextureButton extends Button implements AutoCloseable {
    private static final int CARD_OVERLAY_Z = 3500;

    private final Hash256 modelHash;
    private final String path;
    private final String texture;
    private final CustomGuiPlayerEntity entity;
    private final SelectionHandler selection;
    private final ResourceRequest request;
    private @Nullable ResourceLease lease;
    private @Nullable ModelRenderTarget renderTarget;
    private @Nullable Throwable error;
    private boolean closed;

    public CatalogTextureButton(int x, int y, Hash256 modelHash, String path, String texture,
                                CustomGuiPlayerEntity entity, SelectionHandler selection) {
        super(x, y, 54, 102, Component.literal(texture), ignored -> { }, DEFAULT_NARRATION);
        this.modelHash = modelHash;
        this.path = path;
        this.texture = texture;
        this.entity = entity;
        this.selection = selection;
        var service = ClientModelService.instance();
        request = service.resourceRequest(modelHash, texture);
        lease = service.getOrStart(request);
    }

    @Override
    public void onPress() {
        selection.select(modelHash, path, texture, renderTarget);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateLazyFailure();
        graphics.fillGradient(getX(), getY(), getX() + width, getY() + height, 0xFF434242, 0xFF434242);
        if (renderTarget == null || error != null) {
            var text = error == null ? ".".repeat((int) ((Util.getMillis() / 250L) % 4L)) : "!";
            graphics.drawCenteredString(Minecraft.getInstance().font, text, getX() + width / 2,
                    getY() + (height - 20) / 2, error == null ? 0xFFF3EFE0 : 0xFFFF5555);
        } else {
            renderEntity(graphics);
        }
        renderName(graphics);
        if (isHoveredOrFocused()) {
            graphics.fillGradient(getX(), getY() + 1, getX() + 1, getY() + height - 1,
                    CARD_OVERLAY_Z, 0xFFF3EFE0, 0xFFF3EFE0);
            graphics.fillGradient(getX(), getY(), getX() + width, getY() + 1,
                    CARD_OVERLAY_Z, 0xFFF3EFE0, 0xFFF3EFE0);
            graphics.fillGradient(getX() + width - 1, getY() + 1, getX() + width, getY() + height - 1,
                    CARD_OVERLAY_Z, 0xFFF3EFE0, 0xFFF3EFE0);
            graphics.fillGradient(getX(), getY() + height - 1, getX() + width, getY() + height,
                    CARD_OVERLAY_Z, 0xFFF3EFE0, 0xFFF3EFE0);
        }
    }

    private void updateLazyFailure() {
        pollLease();
        if (lease != null && !lease.isCurrent(request)) {
            releaseLease();
            renderTarget = null;
            error = new IllegalStateException("The model content changed while this page was open");
            return;
        }
        if (error != null || renderTarget == null || renderTarget.playerResources() == null) {
            return;
        }
        var resources = renderTarget.playerResources();
        if (resources.animations().hasFailures()
                || resources.fpArmAnimations().hasFailures()) {
            error = new IllegalStateException("One or more preview animations failed to load");
        }
        if (error != null) {
            releaseLease();
            renderTarget = null;
        }
    }

    private void pollLease() {
        if (closed || lease == null || renderTarget != null || error != null) {
            return;
        }
        var result = lease.poll();
        if (result instanceof AcquireResult.Failed failed) {
            error = failed.failure().cause();
            releaseLease();
        } else if (result instanceof AcquireResult.Ready ready) {
            renderTarget = ready.target();
            entity.reset();
            entity.getPreviewInfo().setPreview(AnimationRegister.IDLE);
            entity.updateModelAndTexture(modelHash, texture);
        }
    }

    @Override
    public void close() {
        closed = true;
        entity.reset();
        releaseLease();
        renderTarget = null;
    }

    private void releaseLease() {
        var current = lease;
        lease = null;
        if (current != null) {
            current.cancelPending();
        }
    }

    private void renderEntity(GuiGraphics graphics) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        RenderSystem.enableScissor((int) (getX() * scale),
                (int) (window.getHeight() - ((getY() + height - 20) * scale)),
                (int) (width * scale), (int) ((height - 20) * scale));
        RenderUtil.renderModelInGui(getX() + width / 2f, getY() + height / 2f + 24f, 35f,
                Minecraft.getInstance().getFrameTime(), entity,
                RegisterEntityRenderersEvent.getPlayerRenderer(), false, true);
        RenderSystem.disableScissor();
    }

    private void renderName(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        var name = renderTarget == null ? texture
                : LanguageManager.getI18n(renderTarget, "files.player.texture.%s".formatted(texture), texture);
        List<FormattedCharSequence> split = font.split(Component.literal(name), 50);
        if (split.size() > 1) {
            graphics.drawCenteredString(font, split.get(0), getX() + width / 2, getY() + height - 19, 0xF3EFE0);
            graphics.drawCenteredString(font, split.get(1), getX() + width / 2, getY() + height - 10, 0xF3EFE0);
        } else {
            graphics.drawCenteredString(font, Component.literal(name), getX() + width / 2,
                    getY() + height - 15, 0xF3EFE0);
        }
    }

    @FunctionalInterface
    public interface SelectionHandler {
        void select(Hash256 hash, String path, String texture, @Nullable ModelRenderTarget renderTarget);
    }
}
