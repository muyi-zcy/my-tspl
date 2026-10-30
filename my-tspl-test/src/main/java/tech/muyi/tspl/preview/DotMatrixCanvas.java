package tech.muyi.tspl.preview;

import tech.muyi.tspl.cmd.BitmapCmd;
import tech.muyi.tspl.render.MonoBitmap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 标签点阵画布：true=黑（打印），false=白。
 * <p>
 * 将 BITMAP / LINE / BOX 合成后输出为 PNG 预览图。
 */
public final class DotMatrixCanvas {

    private final int width;
    private final int height;
    private final boolean[] dots;

    public DotMatrixCanvas(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("canvas size must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.dots = new boolean[width * height];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean get(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return false;
        }
        return dots[y * width + x];
    }

    public void set(int x, int y, boolean black) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        dots[y * width + x] = black;
    }

    /**
     * 将 BITMAP 盖印到画布。mode：0 覆盖，1 OR（默认），2 XOR。
     */
    public void stamp(BitmapCmd cmd) {
        if (cmd == null) {
            return;
        }
        int ox = cmd.x();
        int oy = cmd.y();
        int wb = cmd.widthBytes();
        int h = cmd.height();
        int mode = cmd.mode();
        byte[] data = cmd.data();
        int bw = MonoBitmap.widthDots(wb);
        for (int y = 0; y < h; y++) {
            int cy = oy + y;
            if (cy < 0 || cy >= height) {
                continue;
            }
            for (int x = 0; x < bw; x++) {
                int cx = ox + x;
                if (cx < 0 || cx >= width) {
                    continue;
                }
                boolean srcBlack = MonoBitmap.isBlack(data, wb, x, y);
                int idx = cy * width + cx;
                if (mode == 0) {
                    dots[idx] = srcBlack;
                } else if (mode == 2) {
                    if (srcBlack) {
                        dots[idx] = !dots[idx];
                    }
                } else if (srcBlack) {
                    dots[idx] = true;
                }
            }
        }
    }

    public void fillRect(int x0, int y0, int x1, int y1, boolean black) {
        int left = Math.min(x0, x1);
        int right = Math.max(x0, x1);
        int top = Math.min(y0, y1);
        int bottom = Math.max(y0, y1);
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                set(x, y, black);
            }
        }
    }

    /**
     * Bresenham 直线，thickness 向两侧扩展。
     */
    public void drawLine(int x0, int y0, int x1, int y1, int thickness) {
        int t = thickness < 1 ? 1 : thickness;
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            stampThick(x, y, t);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    public void drawBox(int x0, int y0, int x1, int y1, int thickness) {
        int t = thickness < 1 ? 1 : thickness;
        drawLine(x0, y0, x1, y0, t);
        drawLine(x0, y1, x1, y1, t);
        drawLine(x0, y0, x0, y1, t);
        drawLine(x1, y0, x1, y1, t);
    }

    public BufferedImage toImage() {
        return DotMatrixImage.render(this);
    }

    public BufferedImage toImage(ImagePreviewOptions options) {
        return DotMatrixImage.render(this, options);
    }

    public byte[] toPngBytes() {
        return DotMatrixImage.toPngBytes(this, ImagePreviewOptions.defaults());
    }

    public byte[] toPngBytes(ImagePreviewOptions options) {
        return DotMatrixImage.toPngBytes(this, options);
    }

    public Path writePng(Path path) throws IOException {
        return DotMatrixImage.writePng(this, path, ImagePreviewOptions.defaults());
    }

    public Path writePng(Path path, ImagePreviewOptions options) throws IOException {
        return DotMatrixImage.writePng(this, path, options);
    }

    private void stampThick(int x, int y, int thickness) {
        int half = thickness / 2;
        for (int dy = -half; dy <= half; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                set(x + dx, y + dy, true);
            }
        }
    }
}
