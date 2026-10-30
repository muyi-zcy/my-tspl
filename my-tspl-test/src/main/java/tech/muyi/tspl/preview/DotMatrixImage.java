package tech.muyi.tspl.preview;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 将点阵画布渲染为位图（PNG），比字符预览细腻，适合 IDE / 浏览器查看。
 */
public final class DotMatrixImage {

    private DotMatrixImage() {
    }

    public static BufferedImage render(DotMatrixCanvas canvas) {
        return render(canvas, ImagePreviewOptions.defaults());
    }

    public static BufferedImage render(DotMatrixCanvas canvas, ImagePreviewOptions options) {
        if (canvas == null) {
            throw new IllegalArgumentException("canvas is null");
        }
        ImagePreviewOptions o = options == null ? ImagePreviewOptions.defaults() : options;
        int zoom = o.zoom();
        int pad = o.padding();
        int imgW = canvas.width() * zoom + pad * 2;
        int imgH = canvas.height() * zoom + pad * 2;
        BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(o.paper());
            g.fillRect(0, 0, imgW, imgH);
            g.setColor(o.ink());
            if (o.roundDots() && zoom >= 2) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double diameter = zoom * o.fillRatio();
                double offset = (zoom - diameter) / 2.0;
                for (int y = 0; y < canvas.height(); y++) {
                    for (int x = 0; x < canvas.width(); x++) {
                        if (!canvas.get(x, y)) {
                            continue;
                        }
                        double px = pad + x * zoom + offset;
                        double py = pad + y * zoom + offset;
                        g.fillOval((int) Math.floor(px), (int) Math.floor(py),
                                (int) Math.ceil(diameter), (int) Math.ceil(diameter));
                    }
                }
            } else {
                int fill = Math.max(1, (int) Math.round(zoom * o.fillRatio()));
                int inset = (zoom - fill) / 2;
                for (int y = 0; y < canvas.height(); y++) {
                    for (int x = 0; x < canvas.width(); x++) {
                        if (!canvas.get(x, y)) {
                            continue;
                        }
                        g.fillRect(pad + x * zoom + inset, pad + y * zoom + inset, fill, fill);
                    }
                }
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    public static byte[] toPngBytes(DotMatrixCanvas canvas, ImagePreviewOptions options) {
        BufferedImage image = render(canvas, options);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("PNG writer not available");
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to encode PNG", e);
        }
        return out.toByteArray();
    }

    public static Path writePng(DotMatrixCanvas canvas, Path path, ImagePreviewOptions options)
            throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        BufferedImage image = render(canvas, options);
        try (OutputStream out = Files.newOutputStream(path)) {
            if (!ImageIO.write(image, "png", out)) {
                throw new IOException("PNG writer not available");
            }
        }
        return path;
    }
}
