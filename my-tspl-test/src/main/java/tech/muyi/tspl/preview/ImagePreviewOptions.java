package tech.muyi.tspl.preview;

import java.awt.Color;

/**
 * 点阵 → 位图预览参数（像素级，可写 PNG）。
 */
public final class ImagePreviewOptions {

    private final int zoom;
    private final boolean roundDots;
    private final double fillRatio;
    private final Color paper;
    private final Color ink;
    private final int padding;

    private ImagePreviewOptions(int zoom, boolean roundDots, double fillRatio,
                                Color paper, Color ink, int padding) {
        this.zoom = Math.max(1, zoom);
        this.roundDots = roundDots;
        this.fillRatio = clamp(fillRatio, 0.2, 1.0);
        this.paper = paper == null ? new Color(0xF7, 0xF2, 0xE8) : paper;
        this.ink = ink == null ? new Color(0x1A, 0x1A, 0x1A) : ink;
        this.padding = Math.max(0, padding);
    }

    /**
     * 默认：2× 放大、圆形墨点、热敏纸米色底。
     */
    public static ImagePreviewOptions defaults() {
        return new ImagePreviewOptions(2, true, 0.82,
                new Color(0xF7, 0xF2, 0xE8), new Color(0x1A, 0x1A, 0x1A), 4);
    }

    /** 每个点对应 {@code zoom}×{@code zoom} 像素。 */
    public ImagePreviewOptions zoom(int zoom) {
        return new ImagePreviewOptions(zoom, roundDots, fillRatio, paper, ink, padding);
    }

    /** true=圆形墨点（更接近热敏/针打观感）；false=实心方格。 */
    public ImagePreviewOptions roundDots(boolean roundDots) {
        return new ImagePreviewOptions(zoom, roundDots, fillRatio, paper, ink, padding);
    }

    /** 墨点直径相对单元格的比例（0.2–1.0）。 */
    public ImagePreviewOptions fillRatio(double fillRatio) {
        return new ImagePreviewOptions(zoom, roundDots, fillRatio, paper, ink, padding);
    }

    public ImagePreviewOptions paper(Color paper) {
        return new ImagePreviewOptions(zoom, roundDots, fillRatio, paper, ink, padding);
    }

    public ImagePreviewOptions ink(Color ink) {
        return new ImagePreviewOptions(zoom, roundDots, fillRatio, paper, ink, padding);
    }

    public ImagePreviewOptions padding(int paddingPx) {
        return new ImagePreviewOptions(zoom, roundDots, fillRatio, paper, ink, paddingPx);
    }

    public int zoom() {
        return zoom;
    }

    public boolean roundDots() {
        return roundDots;
    }

    public double fillRatio() {
        return fillRatio;
    }

    public Color paper() {
        return paper;
    }

    public Color ink() {
        return ink;
    }

    public int padding() {
        return padding;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
