package tech.muyi.tspl;

/**
 * TSPL 坐标单位是「点」（dot），点数随打印机 DPI 变化：
 * <ul>
 *   <li>203dpi ≈ 8 dots/mm</li>
 *   <li>300dpi ≈ 12 dots/mm</li>
 * </ul>
 * {@code SIZE} 用毫米描述物理尺寸；BOX / TEXT / QRCODE / BITMAP 的 x,y 仍是点。
 * 同一套按 203dpi 写的坐标打到 300dpi 机上，只会约占满幅的 8/12 ≈ 2/3。
 */
public final class TsplDpi {

    public static final int DPI_203 = 203;
    public static final int DPI_300 = 300;

    public static final int DOTS_PER_MM_203 = 8;
    public static final int DOTS_PER_MM_300 = 12;

    private final int dpi;
    private final int dotsPerMm;

    private TsplDpi(int dpi, int dotsPerMm) {
        this.dpi = dpi;
        this.dotsPerMm = dotsPerMm;
    }

    public static TsplDpi of(int dpi) {
        if (dpi >= 280) {
            return new TsplDpi(DPI_300, DOTS_PER_MM_300);
        }
        return new TsplDpi(DPI_203, DOTS_PER_MM_203);
    }

    public static TsplDpi dpi203() {
        return of(DPI_203);
    }

    public static TsplDpi dpi300() {
        return of(DPI_300);
    }

    public int dpi() {
        return dpi;
    }

    public int dotsPerMm() {
        return dotsPerMm;
    }

    /** 毫米 → 点。 */
    public int mm(double mm) {
        return (int) Math.round(mm * dotsPerMm);
    }

    /**
     * 把「按 203dpi 设计的点坐标」换算到当前 DPI（300dpi 时 ×1.5）。
     * 字号、QR cell、图片 maxWidth 等点阵尺寸同样适用。
     */
    public int dots(int dotsAt203) {
        if (dotsPerMm == DOTS_PER_MM_203) {
            return dotsAt203;
        }
        return (int) Math.round(dotsAt203 * (dotsPerMm / (double) DOTS_PER_MM_203));
    }

    @Override
    public String toString() {
        return "TsplDpi{dpi=" + dpi + ", dotsPerMm=" + dotsPerMm + '}';
    }
}
