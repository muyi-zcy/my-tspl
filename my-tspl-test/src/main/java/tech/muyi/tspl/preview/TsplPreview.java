package tech.muyi.tspl.preview;

import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.TsplDpi;
import tech.muyi.tspl.cmd.BitmapCmd;
import tech.muyi.tspl.cmd.TextLineCmd;
import tech.muyi.tspl.cmd.TsplCmd;
import tech.muyi.tspl.render.MonoBitmap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 {@link TsplDocument} / {@link BitmapCmd} 渲染为 PNG 点阵预览，便于单测目视核对。
 * <p>
 * 支持：BITMAP（mode 0/1/2）、LINE、BOX、QRCODE（ZXing 真实二维码）；
 * SIZE（mm）按文档 {@link TsplDocument#dpi()} 推算画布（未设置时默认 203dpi=8 dots/mm）。
 * BARCODE / TEXT 等机内指令仍以占位矩形标注。
 */
public final class TsplPreview {

    /** 203dpi ≈ 8 dots per mm。 */
    public static final int DOTS_PER_MM = TsplDpi.DOTS_PER_MM_203;

    private static final Pattern SIZE_MM = Pattern.compile(
            "^SIZE\\s+([0-9.]+)\\s*mm\\s*,\\s*([0-9.]+)\\s*mm\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE = Pattern.compile(
            "^LINE\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BOX = Pattern.compile(
            "^BOX\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QRCODE = Pattern.compile(
            "^QRCODE\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,.*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BARCODE = Pattern.compile(
            "^BARCODE\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,.*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXT = Pattern.compile(
            "^TEXT\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,.*$",
            Pattern.CASE_INSENSITIVE);

    private TsplPreview() {
    }

    /**
     * 按给定画布尺寸渲染整份文档。
     */
    public static DotMatrixCanvas render(TsplDocument doc, int widthDots, int heightDots) {
        DotMatrixCanvas canvas = new DotMatrixCanvas(widthDots, heightDots);
        if (doc == null) {
            return canvas;
        }
        paint(doc.commands(), canvas);
        return canvas;
    }

    /**
     * 自动推算画布：优先 SIZE mm + 文档 DPI；否则包络全部 BITMAP / LINE / BOX。
     */
    public static DotMatrixCanvas render(TsplDocument doc) {
        return render(doc, dotsPerMmOf(doc));
    }

    /**
     * 按指定 dots/mm 推算 SIZE（300dpi 传 12，203dpi 传 8）。
     */
    public static DotMatrixCanvas render(TsplDocument doc, int dotsPerMm) {
        int[] size = resolveSize(doc, dotsPerMm);
        return render(doc, size[0], size[1]);
    }

    public static DotMatrixCanvas render(BitmapCmd cmd) {
        return canvasOf(cmd);
    }

    public static BufferedImage toImage(TsplDocument doc) {
        return render(doc).toImage();
    }

    public static BufferedImage toImage(TsplDocument doc, ImagePreviewOptions options) {
        return render(doc).toImage(options);
    }

    public static BufferedImage toImage(BitmapCmd cmd) {
        return canvasOf(cmd).toImage();
    }

    public static BufferedImage toImage(BitmapCmd cmd, ImagePreviewOptions options) {
        return canvasOf(cmd).toImage(options);
    }

    public static Path toPng(TsplDocument doc, Path path) throws IOException {
        return render(doc).writePng(path);
    }

    public static Path toPng(TsplDocument doc, Path path, ImagePreviewOptions options)
            throws IOException {
        return render(doc).writePng(path, options);
    }

    public static Path toPng(TsplDocument doc, Path path, ImagePreviewOptions options, int dotsPerMm)
            throws IOException {
        return render(doc, dotsPerMm).writePng(path, options);
    }

    public static Path toPng(BitmapCmd cmd, Path path) throws IOException {
        return canvasOf(cmd).writePng(path);
    }

    public static Path toPng(BitmapCmd cmd, Path path, ImagePreviewOptions options)
            throws IOException {
        return canvasOf(cmd).writePng(path, options);
    }

    /**
     * 从文档解析标签尺寸（点）。未声明 SIZE 时按内容包围盒 + 边距。
     * 优先使用 {@link TsplDocument#dpi()}。
     */
    public static int[] resolveSize(TsplDocument doc) {
        return resolveSize(doc, dotsPerMmOf(doc));
    }

    /**
     * @param dotsPerMm 8=203dpi，12=300dpi；须与排版坐标所用 DPI 一致
     */
    public static int[] resolveSize(TsplDocument doc, int dotsPerMm) {
        int dpm = Math.max(1, dotsPerMm);
        if (doc != null) {
            for (TsplCmd cmd : doc.commands()) {
                if (!(cmd instanceof TextLineCmd)) {
                    continue;
                }
                Matcher m = SIZE_MM.matcher(((TextLineCmd) cmd).line().trim());
                if (m.matches()) {
                    double wMm = Double.parseDouble(m.group(1));
                    double hMm = Double.parseDouble(m.group(2));
                    int w = Math.max(1, (int) Math.round(wMm * dpm));
                    int h = Math.max(1, (int) Math.round(hMm * dpm));
                    return new int[]{w, h};
                }
            }
        }
        return boundsOrDefault(doc);
    }

    private static void paint(List<TsplCmd> commands, DotMatrixCanvas canvas) {
        for (TsplCmd cmd : commands) {
            if (cmd instanceof BitmapCmd) {
                canvas.stamp((BitmapCmd) cmd);
                continue;
            }
            if (!(cmd instanceof TextLineCmd)) {
                continue;
            }
            String line = ((TextLineCmd) cmd).line();
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            Matcher m;
            m = LINE.matcher(trimmed);
            if (m.matches()) {
                canvas.drawLine(i(m, 1), i(m, 2), i(m, 3), i(m, 4), i(m, 5));
                continue;
            }
            m = BOX.matcher(trimmed);
            if (m.matches()) {
                canvas.drawBox(i(m, 1), i(m, 2), i(m, 3), i(m, 4), i(m, 5));
                continue;
            }
            m = QRCODE.matcher(trimmed);
            if (m.matches()) {
                if (!QrPreviewRenderer.paint(trimmed, canvas)) {
                    markPlaceholder(canvas, i(m, 1), i(m, 2), 80, "QR");
                }
                continue;
            }
            m = BARCODE.matcher(trimmed);
            if (m.matches()) {
                markPlaceholder(canvas, i(m, 1), i(m, 2), 120, 40, "BC");
                continue;
            }
            m = TEXT.matcher(trimmed);
            if (m.matches()) {
                markPlaceholder(canvas, i(m, 1), i(m, 2), 48, 16, "T");
            }
        }
    }

    private static void markPlaceholder(DotMatrixCanvas canvas, int x, int y, int size, String tag) {
        markPlaceholder(canvas, x, y, size, size, tag);
    }

    private static void markPlaceholder(DotMatrixCanvas canvas, int x, int y,
                                        int w, int h, String tag) {
        canvas.drawBox(x, y, x + w - 1, y + h - 1, 1);
        canvas.drawLine(x, y, x + w - 1, y + h - 1, 1);
        if (tag != null && !tag.isEmpty()) {
            for (int i = 0; i < Math.min(tag.length() * 2, w / 2); i++) {
                canvas.set(x + 2 + i, y + 2, true);
            }
        }
    }

    private static int[] boundsOrDefault(TsplDocument doc) {
        int maxX = 0;
        int maxY = 0;
        if (doc != null) {
            for (TsplCmd cmd : doc.commands()) {
                if (cmd instanceof BitmapCmd) {
                    BitmapCmd b = (BitmapCmd) cmd;
                    maxX = Math.max(maxX, b.x() + MonoBitmap.widthDots(b.widthBytes()));
                    maxY = Math.max(maxY, b.y() + b.height());
                    continue;
                }
                if (!(cmd instanceof TextLineCmd)) {
                    continue;
                }
                String trimmed = ((TextLineCmd) cmd).line().trim();
                Matcher m = LINE.matcher(trimmed);
                if (m.matches()) {
                    maxX = Math.max(maxX, Math.max(i(m, 1), i(m, 3)) + 1);
                    maxY = Math.max(maxY, Math.max(i(m, 2), i(m, 4)) + 1);
                    continue;
                }
                m = BOX.matcher(trimmed);
                if (m.matches()) {
                    maxX = Math.max(maxX, Math.max(i(m, 1), i(m, 3)) + 1);
                    maxY = Math.max(maxY, Math.max(i(m, 2), i(m, 4)) + 1);
                }
            }
        }
        if (maxX <= 0 || maxY <= 0) {
            return new int[]{64, 64};
        }
        return new int[]{maxX + 8, maxY + 8};
    }

    private static int i(Matcher m, int group) {
        return Integer.parseInt(m.group(group));
    }

    private static int dotsPerMmOf(TsplDocument doc) {
        if (doc != null && doc.dpi() != null) {
            return doc.dpi().dotsPerMm();
        }
        return DOTS_PER_MM;
    }

    private static DotMatrixCanvas canvasOf(BitmapCmd cmd) {
        if (cmd == null) {
            return new DotMatrixCanvas(1, 1);
        }
        DotMatrixCanvas canvas = new DotMatrixCanvas(
                Math.max(1, MonoBitmap.widthDots(cmd.widthBytes())),
                Math.max(1, cmd.height()));
        canvas.stamp(new BitmapCmd(0, 0, cmd.widthBytes(), cmd.height(), cmd.mode(), cmd.data()));
        return canvas;
    }
}
