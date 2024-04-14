package earth.terrarium.argonauts.client.rendering;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.common.utils.WebUtils;
import earth.terrarium.argonauts.Argonauts;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UrlTexture extends SimpleTexture {

    private static final Map<String, Info> INFO = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    private static final Info DEFAULT_INFO = new Info(24, 24, 24, 24);

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(Argonauts.MOD_ID, "textures/gui/hourglass.png");

    private final String url;
    private boolean loaded;
    private CompletableFuture<?> loader;

    public UrlTexture(String url) {
        super(DEFAULT_TEXTURE);
        this.url = url;
    }

    @SuppressWarnings({"deprecation"})
    public static ResourceLocation getTextureId(String url) {
        return new ResourceLocation(Argonauts.MOD_ID, "urlimages/" + Hashing.sha1().hashUnencodedChars(url));
    }

    public static Info getInfo(String url) {
        return INFO.getOrDefault(url, DEFAULT_INFO);
    }

    private void upload(NativeImage image) {
        TextureUtil.prepareImage(this.getId(), image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, true);
        INFO.put(this.url, Info.from(image));
    }

    @Override
    public void load(@NotNull ResourceManager manager) {
        Minecraft.getInstance().execute(() -> {
            if (!this.loaded) {
                try {
                    super.load(manager);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.loaded = true;
            }
        });

        if (this.loader == null) {
            this.loader = CompletableFuture.runAsync(() ->
                    WebUtils.get(this.url, HttpResponse.BodyHandlers.ofInputStream())
                        .ifPresent(response -> {
                            if (response.statusCode() / 100 == 2) {
                                NativeImage image = this.loadTexture(response.body());
                                Minecraft.getInstance().execute(() -> {
                                    if (image != null) {
                                        Minecraft.getInstance().execute(() -> {
                                            this.loaded = true;
                                            if (!RenderSystem.isOnRenderThread()) {
                                                RenderSystem.recordRenderCall(() -> this.upload(image));
                                            } else {
                                                this.upload(image);
                                            }
                                        });
                                    }
                                });
                            }
                        }),
                Util.backgroundExecutor()
            );
        }
    }

    @Nullable
    private NativeImage loadTexture(InputStream stream) {
        NativeImage nativeImage = null;

        try {
            nativeImage = NativeImage.read(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nativeImage;
    }

    public record Info(int width, int height, int displayWidth, int displayHeight) {

        public static Info from(NativeImage image) {
            double widthMultiple = Math.ceil((double) image.getWidth() / (double) 150);
            double heightMultiple = Math.ceil((double) image.getHeight() / (double) 100);
            int ratio = (int) Math.max(widthMultiple, heightMultiple);

            return new Info(image.getWidth(), image.getHeight(), image.getWidth() / ratio, image.getHeight() / ratio);
        }
    }
}