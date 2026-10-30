package tech.muyi.tspl.render;

import tech.muyi.tspl.TsplException;
import tech.muyi.tspl.builder.ImageOptions;
import tech.muyi.tspl.cmd.BitmapCmd;
import tech.muyi.tspl.cmd.TsplCmd;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 将图片解码、缩放、阈值/抖动后打包为内联 TSPL BITMAP。
 * <p>支持 {@link BufferedImage}、字节、流、本地路径与 http(s)/file URL。</p>
 */
public final class ImageBitmapRenderer {

    private ImageBitmapRenderer() {
    }

    public static TsplCmd command(int x, int y, BufferedImage image) {
        return command(x, y, image, ImageOptions.defaults());
    }

    public static TsplCmd command(int x, int y, BufferedImage image, ImageOptions options) {
        if (image == null) {
            return out -> {
            };
        }
        ImageOptions opts = options == null ? ImageOptions.defaults() : options;
        BufferedImage scaled = scale(image, opts);
        byte[] packed = opts.dither()
                ? packDithered(scaled, opts.threshold())
                : MonoBitmap.pack(scaled, opts.threshold());
        int widthBytes = MonoBitmap.widthBytes(scaled.getWidth());
        return new BitmapCmd(x, y, widthBytes, scaled.getHeight(), opts.bitmapMode(), packed);
    }

    public static TsplCmd command(int x, int y, byte[] imageBytes, ImageOptions options) {
        return command(x, y, decode(imageBytes), options);
    }

    public static TsplCmd command(int x, int y, InputStream in, ImageOptions options) {
        return command(x, y, decode(in), options);
    }

    /**
     * 从图片地址加载：http(s)/file URL，或本地文件路径。
     */
    public static TsplCmd command(int x, int y, String location, ImageOptions options) {
        return command(x, y, load(location), options);
    }

    public static TsplCmd command(int x, int y, URL url, ImageOptions options) {
        return command(x, y, load(url), options);
    }

    public static TsplCmd command(int x, int y, Path path, ImageOptions options) {
        return command(x, y, load(path), options);
    }

    public static BufferedImage decode(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        return decode(new ByteArrayInputStream(imageBytes));
    }

    /**
     * 解码图片流；不关闭调用方传入的流。
     */
    public static BufferedImage decode(InputStream in) {
        if (in == null) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new TsplException("unsupported or corrupt image data");
            }
            return image;
        } catch (IOException e) {
            throw new TsplException("failed to decode image", e);
        }
    }

    /**
     * 加载图片：{@code http://} / {@code https://} / {@code file:} URL，或本地路径。
     */
    public static BufferedImage load(String location) {
        if (location == null) {
            return null;
        }
        String loc = location.trim();
        if (loc.isEmpty()) {
            return null;
        }
        if (looksLikeUrl(loc)) {
            try {
                return load(new URL(loc));
            } catch (MalformedURLException e) {
                throw new TsplException("invalid image url: " + loc, e);
            }
        }
        return load(Paths.get(loc));
    }

    public static BufferedImage load(URL url) {
        if (url == null) {
            return null;
        }
        try (InputStream in = url.openStream()) {
            return decode(in);
        } catch (IOException e) {
            throw new TsplException("failed to load image from " + url, e);
        }
    }

    public static BufferedImage load(Path path) {
        if (path == null) {
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            return decode(in);
        } catch (IOException e) {
            throw new TsplException("failed to load image from " + path, e);
        }
    }

    private static boolean looksLikeUrl(String location) {
        int colon = location.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        String scheme = location.substring(0, colon);
        return "http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme)
                || "file".equalsIgnoreCase(scheme);
    }

    private static BufferedImage scale(BufferedImage src, ImageOptions opts) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int maxW = opts.maxWidth();
        int maxH = opts.maxHeight();
        if (maxW <= 0 && maxH <= 0) {
            return src;
        }

        int targetW = srcW;
        int targetH = srcH;

        if (opts.keepAspect()) {
            double scale = 1.0;
            if (maxW > 0) {
                scale = Math.min(scale, maxW / (double) srcW);
            }
            if (maxH > 0) {
                scale = Math.min(scale, maxH / (double) srcH);
            }
            if (scale >= 1.0 && (maxW <= 0 || srcW <= maxW) && (maxH <= 0 || srcH <= maxH)) {
                return src;
            }
            targetW = Math.max(1, (int) Math.round(srcW * scale));
            targetH = Math.max(1, (int) Math.round(srcH * scale));
        } else {
            if (maxW > 0) {
                targetW = maxW;
            }
            if (maxH > 0) {
                targetH = maxH;
            }
            if (targetW == srcW && targetH == srcH) {
                return src;
            }
        }

        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    /**
     * Floyd–Steinberg 抖动后打包为 TSPL 单色字节。
     */
    private static byte[] packDithered(BufferedImage image, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][] gray = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[y][x] = (r * 30 + g * 59 + b * 11) / 100f;
            }
        }

        int t = Math.max(0, Math.min(255, threshold));
        int widthBytes = MonoBitmap.widthBytes(width);
        byte[] data = new byte[widthBytes * height];
        for (int y = 0; y < height; y++) {
            int row = y * widthBytes;
            for (int x = 0; x < width; x++) {
                float old = gray[y][x];
                float neu = old < t ? 0f : 255f;
                boolean black = neu < t;
                if (!black) {
                    data[row + (x >> 3)] |= (byte) (0x80 >> (x & 7));
                }
                float err = old - neu;
                if (x + 1 < width) {
                    gray[y][x + 1] += err * 7f / 16f;
                }
                if (y + 1 < height) {
                    if (x > 0) {
                        gray[y + 1][x - 1] += err * 3f / 16f;
                    }
                    gray[y + 1][x] += err * 5f / 16f;
                    if (x + 1 < width) {
                        gray[y + 1][x + 1] += err * 1f / 16f;
                    }
                }
            }
        }
        return data;
    }
}
