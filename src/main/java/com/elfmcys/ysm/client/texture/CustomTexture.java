package com.elfmcys.ysm.client.texture;

import com.elfmcys.ysm.YesSteveModel;
import com.elfmcys.ysm.natives.image.ImageSource;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class CustomTexture extends AbstractTexture {
    private final ImageSource source;
    private final Decoder decoder;
    private final Uploader uploader;
    private @Nullable Throwable failure;

    public CustomTexture(ImageSource source) {
        this(source, CustomTexture::decode, CustomTexture::upload);
    }

    CustomTexture(ImageSource source, Decoder decoder, Uploader uploader) {
        this.source = Objects.requireNonNull(source, "source");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.uploader = Objects.requireNonNull(uploader, "uploader");
    }

    @Override
    public void load(ResourceManager resourceManager) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (failure != null) {
            return;
        }
        try (var pixels = decoder.decode(source)) {
            uploader.upload(this, pixels);
        } catch (Exception error) {
            failure = error;
            YesSteveModel.LOGGER.debug("Failed to load standalone GUI texture from {}", source, error);
        }
    }

    private static NativeImage decode(ImageSource source) throws Exception {
        try (var image = source.open()) {
            return image.decode();
        }
    }

    private static void upload(CustomTexture texture, NativeImage img) {
        TextureUtil.prepareImage(texture.getId(), 0, img.getWidth(), img.getHeight());
        img.upload(0, 0, 0, 0, 0,
                img.getWidth(), img.getHeight(),
                false, false, false, false);
    }

    public Optional<Throwable> failure() {
        return Optional.ofNullable(failure);
    }

    @FunctionalInterface
    interface Decoder {
        NativeImage decode(ImageSource source) throws Exception;
    }

    @FunctionalInterface
    interface Uploader {
        void upload(CustomTexture texture, NativeImage image);
    }
}
