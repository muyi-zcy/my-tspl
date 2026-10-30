package tech.muyi.tspl.builder;

/**
 * 服务端图片 → BITMAP 的可选参数。
 */
public final class ImageOptions {

    private final int maxWidth;
    private final int maxHeight;
    private final boolean keepAspect;
    private final int threshold;
    private final int bitmapMode;
    private final boolean dither;

    private ImageOptions(int maxWidth, int maxHeight, boolean keepAspect,
                         int threshold, int bitmapMode, boolean dither) {
        this.maxWidth = Math.max(0, maxWidth);
        this.maxHeight = Math.max(0, maxHeight);
        this.keepAspect = keepAspect;
        this.threshold = Math.max(0, Math.min(255, threshold));
        this.bitmapMode = bitmapMode;
        this.dither = dither;
    }

    public static ImageOptions defaults() {
        return new ImageOptions(0, 0, true, 128, 1, false);
    }

    public static ImageOptions maxWidth(int maxWidthDots) {
        return defaults().withMaxWidth(maxWidthDots);
    }

    public static ImageOptions maxSize(int maxWidthDots, int maxHeightDots) {
        return defaults().withMaxWidth(maxWidthDots).withMaxHeight(maxHeightDots);
    }

    public ImageOptions withMaxWidth(int maxWidthDots) {
        return new ImageOptions(maxWidthDots, maxHeight, keepAspect, threshold, bitmapMode, dither);
    }

    public ImageOptions withMaxHeight(int maxHeightDots) {
        return new ImageOptions(maxWidth, maxHeightDots, keepAspect, threshold, bitmapMode, dither);
    }

    public ImageOptions keepAspect(boolean keep) {
        return new ImageOptions(maxWidth, maxHeight, keep, threshold, bitmapMode, dither);
    }

    /**
     * 灰度阈值 0–255，亮度低于此值视为黑（打印）。
     */
    public ImageOptions threshold(int threshold) {
        return new ImageOptions(maxWidth, maxHeight, keepAspect, threshold, bitmapMode, dither);
    }

    /**
     * BITMAP 模式：0=覆盖（OVERWRITE），1=或（OR），2=异或（XOR）。
     */
    public ImageOptions bitmapMode(int mode) {
        return new ImageOptions(maxWidth, maxHeight, keepAspect, threshold, mode, dither);
    }

    /**
     * 是否使用 Floyd–Steinberg 抖动（默认关闭）。
     */
    public ImageOptions dither(boolean dither) {
        return new ImageOptions(maxWidth, maxHeight, keepAspect, threshold, bitmapMode, dither);
    }

    public int maxWidth() {
        return maxWidth;
    }

    public int maxHeight() {
        return maxHeight;
    }

    public boolean keepAspect() {
        return keepAspect;
    }

    public int threshold() {
        return threshold;
    }

    public int bitmapMode() {
        return bitmapMode;
    }

    public boolean dither() {
        return dither;
    }
}
