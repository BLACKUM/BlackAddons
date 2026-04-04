package org.blackum.blackaddons.feature.chat;

import org.blackum.blackaddons.core.util.Constants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageHelper {
    private static final Map<String, ImageInfo> INFO_CACHE = new ConcurrentHashMap<>();
    private static final Pattern TENOR_IMAGE_PATTERN = Pattern
            .compile("<meta property=\"og:image\" content=\"(https://media\\.tenor\\.com/[^\"]+)\"");

    public record FrameInfo(Identifier location, int delay) {
    }

    public record ImageInfo(List<FrameInfo> frames, int width, int height) {
    }

    public static CompletableFuture<ImageInfo> downloadImage(String urlStr) {
        return downloadImage(urlStr, null);
    }

    public static CompletableFuture<ImageInfo> downloadImage(String urlStr, Consumer<Float> progressCallback) {
        if (urlStr == null)
            return CompletableFuture.completedFuture(null);

        final String cleanedUrl = urlStr.split(" ")[0].split("§")[0].trim();

        if (INFO_CACHE.containsKey(cleanedUrl)) {
            return CompletableFuture.completedFuture(INFO_CACHE.get(cleanedUrl));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String targetUrl = cleanedUrl;
                if (cleanedUrl.contains("tenor.com/view/")) {
                    targetUrl = resolveTenorUrl(cleanedUrl);
                    if (targetUrl == null) {
                        return null;
                    }
                }

                final String finalTargetUrl = targetUrl;
                URL url = URI.create(finalTargetUrl).toURL();
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("User-Agent", Constants.BROWSER_USER_AGENT);
                connection.setConnectTimeout(Constants.HTTP_TIMEOUT_SECONDS * 1000);
                connection.setReadTimeout(Constants.HTTP_TIMEOUT_SECONDS * 1000);

                int contentLength = connection.getContentLength();
                try (InputStream in = connection.getInputStream()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[Constants.BUFFER_SIZE];
                    int n;
                    int downloaded = 0;
                    while ((n = in.read(buffer)) != -1) {
                        bos.write(buffer, 0, n);
                        downloaded += n;
                        if (progressCallback != null && contentLength > 0) {
                            progressCallback.accept((float) downloaded / contentLength);
                        }
                    }
                    byte[] imageBytes = bos.toByteArray();
                    if (progressCallback != null) {
                        progressCallback.accept(1.0f);
                    }

                    CompletableFuture<ImageInfo> future = new CompletableFuture<>();
                    Minecraft.getInstance().execute(() -> {
                        try {
                            List<FrameInfo> frames = new ArrayList<>();
                            int width = 0;
                            int height = 0;

                            if (finalTargetUrl.toLowerCase().contains(".gif")) {
                                frames = decodeGif(imageBytes, cleanedUrl);
                            }

                            if (frames.isEmpty()) {
                                NativeImage nativeImage = decodeToNativeImage(imageBytes);
                                if (nativeImage != null) {
                                    width = nativeImage.getWidth();
                                    height = nativeImage.getHeight();
                                    Identifier location = registerTexture(nativeImage, cleanedUrl, 0);
                                    frames.add(new FrameInfo(location, 0));
                                }
                            }

                            if (frames.isEmpty()) {
                                future.complete(null);
                                return;
                            }

                            if (width == 0 || height == 0) {
                                NativeImage firstFrame = decodeToNativeImage(imageBytes);
                                if (firstFrame != null) {
                                    width = firstFrame.getWidth();
                                    height = firstFrame.getHeight();
                                }
                            }

                            ImageInfo info = new ImageInfo(frames, width, height);
                            INFO_CACHE.put(cleanedUrl, info);
                            future.complete(info);
                        } catch (Exception e) {
                            future.complete(null);
                        }
                    });

                    return future.join();
                }
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static String resolveTenorUrl(String tenorUrl) {
        try {
            URL url = URI.create(tenorUrl).toURL();
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", Constants.BROWSER_USER_AGENT);
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[Constants.BUFFER_SIZE];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, n);
                }
                String html = bos.toString();
                Matcher matcher = TENOR_IMAGE_PATTERN.matcher(html);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<FrameInfo> decodeGif(byte[] bytes, String originalUrl) {
        List<FrameInfo> frames = new ArrayList<>();
        try (ImageInputStream is = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext())
                return frames;

            ImageReader reader = readers.next();
            reader.setInput(is);

            int numFrames = reader.getNumImages(true);
            for (int i = 0; i < numFrames; i++) {
                BufferedImage bi = reader.read(i);
                int delay = Constants.DEFAULT_GIF_DELAY;

                try {
                    IIOMetadata metadata = reader.getImageMetadata(i);
                    String metaFormat = metadata.getNativeMetadataFormatName();
                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormat);
                    IIOMetadataNode gce = findNode(root, "GraphicControlExtension");
                    if (gce != null) {
                        String delayStr = gce.getAttribute("delayTime");
                        if (delayStr != null && !delayStr.isEmpty()) {
                            delay = Integer.parseInt(delayStr) * 10;
                        }
                    }
                } catch (Exception ignored) {
                }

                NativeImage ni = fromBufferedImage(bi);
                Identifier loc = registerTexture(ni, originalUrl, i);
                frames.add(new FrameInfo(loc, delay > 0 ? delay : Constants.DEFAULT_GIF_DELAY));
            }
        } catch (Exception ignored) {
        }
        return frames;
    }

    private static IIOMetadataNode findNode(IIOMetadataNode root, String nodeName) {
        int nNodes = root.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        return null;
    }

    private static NativeImage fromBufferedImage(BufferedImage bi) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        NativeImage ni = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = bi.getRGB(x, y);
                int abgr = (argb & 0xFF00FF00) | ((argb & 0x00FF0000) >> 16) | ((argb & 0x000000FF) << 16);
                ni.setPixelABGR(x, y, abgr);
            }
        }
        return ni;
    }

    private static NativeImage decodeToNativeImage(byte[] bytes) {
        try {
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bi != null)
                    return fromBufferedImage(bi);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Identifier registerTexture(NativeImage ni, String url, int frame) {
        String label = "img_" + Math.abs(url.hashCode()) + "_" + frame;
        DynamicTexture texture = new DynamicTexture(() -> label, ni);
        Identifier loc = Identifier.fromNamespaceAndPath(Constants.MOD_ID, label.toLowerCase());
        Minecraft.getInstance().getTextureManager().register(loc, texture);
        return loc;
    }

    public static void clearCache() {
        INFO_CACHE.values().forEach(info -> {
            info.frames().forEach(f -> Minecraft.getInstance().getTextureManager().release(f.location()));
        });
        INFO_CACHE.clear();
    }
}
