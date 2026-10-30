package tech.muyi.tspl.builder;

import tech.muyi.tspl.TsplCharsets;
import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.TsplDpi;
import tech.muyi.tspl.cmd.TsplCmd;
import tech.muyi.tspl.cmd.TsplCmds;
import tech.muyi.tspl.encode.TsplEncoder;
import tech.muyi.tspl.render.ImageBitmapRenderer;
import tech.muyi.tspl.render.TextBitmapRenderer;
import tech.muyi.tspl.render.TextStyle;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 单张标签任务的流式构建器。调用顺序即打印图层顺序。
 * <p>
 * 通过 {@link #dpi(int)} 声明打印机分辨率后，可用 {@link #mm(double)} / {@link #dots(int)}
 * 换算坐标与尺寸；默认 203dpi。
 */
public final class TsplLabel {

    private final List<TsplCmd> commands = new ArrayList<>();
    private Charset charset = TsplCharsets.GB2312;
    private TsplDpi dpi = TsplDpi.dpi203();

    private TsplLabel() {
    }

    public static TsplLabel create() {
        return new TsplLabel();
    }

    public TsplLabel charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    /**
     * 设置打印机 DPI（常见 203 / 300）。影响 {@link #mm(double)}、{@link #dots(int)}
     * 以及写入 {@link TsplDocument} 的元数据，供预览按正确点数推算画布。
     */
    public TsplLabel dpi(int dpi) {
        this.dpi = TsplDpi.of(dpi);
        return this;
    }

    public TsplLabel dpi(TsplDpi dpi) {
        this.dpi = dpi == null ? TsplDpi.dpi203() : dpi;
        return this;
    }

    public TsplDpi dpi() {
        return dpi;
    }

    /** 毫米 → 点（按当前 {@link #dpi()}）。 */
    public int mm(double mm) {
        return dpi.mm(mm);
    }

    /**
     * 将按 203dpi 设计的点坐标/字号换算到当前 DPI（300dpi 时约为 ×1.5）。
     */
    public int dots(int dotsAt203) {
        return dpi.dots(dotsAt203);
    }

    public TsplLabel append(TsplCmd cmd) {
        if (cmd != null) {
            commands.add(cmd);
        }
        return this;
    }

    public TsplLabel appendRaw(String line) {
        return append(TsplCmds.raw(line));
    }

    public TsplLabel appendRaw(byte[] payload) {
        return append(TsplCmds.rawBytes(payload));
    }

    public TsplLabel cls() {
        return append(TsplCmds.cls());
    }

    public TsplLabel sizeMm(double widthMm, double heightMm) {
        return append(TsplCmds.sizeMm(widthMm, heightMm));
    }

    public TsplLabel sizeInch(double widthInch, double heightInch) {
        return append(TsplCmds.sizeInch(widthInch, heightInch));
    }

    public TsplLabel gapMm(double gapMm, double offsetMm) {
        return append(TsplCmds.gapMm(gapMm, offsetMm));
    }

    public TsplLabel direction(int direction, int mirror) {
        return append(TsplCmds.direction(direction, mirror));
    }

    public TsplLabel reference(int x, int y) {
        return append(TsplCmds.reference(x, y));
    }

    public TsplLabel offsetMm(double offsetMm) {
        return append(TsplCmds.offsetMm(offsetMm));
    }

    public TsplLabel peel(boolean on) {
        return append(TsplCmds.setPeel(on));
    }

    public TsplLabel cutter(boolean on) {
        return append(TsplCmds.setCutter(on));
    }

    public TsplLabel partialCutter(boolean on) {
        return append(TsplCmds.setPartialCutter(on));
    }

    public TsplLabel tear(boolean on) {
        return append(TsplCmds.setTear(on));
    }

    public TsplLabel speed(int ips) {
        return append(TsplCmds.speed(ips));
    }

    public TsplLabel density(int density) {
        return append(TsplCmds.density(density));
    }

    public TsplLabel qrcode(int x, int y, String content) {
        return qrcode(x, y, QrOptions.defaults(), content);
    }

    public TsplLabel qrcode(int x, int y, QrOptions options, String content) {
        QrOptions o = options == null ? QrOptions.defaults() : options;
        return append(TsplCmds.qrcode(x, y, o.ecc(), o.cellWidth(), o.mode(),
                o.rotation(), o.model(), content));
    }

    public TsplLabel barcode(int x, int y, String codeType, int height,
                             int humanReadable, int rotation,
                             int narrow, int wide, String content) {
        return append(TsplCmds.barcode(x, y, codeType, height, humanReadable,
                rotation, narrow, wide, content));
    }

    public TsplLabel text(int x, int y, String font, int rotation,
                          int xMul, int yMul, String content) {
        return append(TsplCmds.text(x, y, font, rotation, xMul, yMul, content));
    }

    public TsplLabel line(int x0, int y0, int x1, int y1, int thickness) {
        return append(TsplCmds.line(x0, y0, x1, y1, thickness));
    }

    public TsplLabel box(int x0, int y0, int x1, int y1, int thickness) {
        return append(TsplCmds.box(x0, y0, x1, y1, thickness));
    }

    public TsplLabel hline(int x, int y, int length, int thickness) {
        return append(TsplCmds.hline(x, y, length, thickness));
    }

    public TsplLabel vline(int x, int y, int length, int thickness) {
        return append(TsplCmds.vline(x, y, length, thickness));
    }

    /**
     * 服务端渲染中文/拉丁文本为内联 BITMAP。
     */
    public TsplLabel textBitmap(int x, int y, String text, TextStyle style) {
        return append(TextBitmapRenderer.command(x, y, text, style));
    }

    public TsplLabel textBitmapCentered(int y, String text, TextStyle style, int labelWidthDots) {
        return append(TextBitmapRenderer.centered(y, text, style, labelWidthDots));
    }

    /**
     * 服务端将图片解码为内联 BITMAP。
     */
    public TsplLabel image(int x, int y, BufferedImage image) {
        return append(ImageBitmapRenderer.command(x, y, image));
    }

    public TsplLabel image(int x, int y, BufferedImage image, ImageOptions options) {
        return append(ImageBitmapRenderer.command(x, y, image, options));
    }

    public TsplLabel image(int x, int y, byte[] pngOrJpeg) {
        return image(x, y, pngOrJpeg, ImageOptions.defaults());
    }

    public TsplLabel image(int x, int y, byte[] pngOrJpeg, ImageOptions options) {
        return append(ImageBitmapRenderer.command(x, y, pngOrJpeg, options));
    }

    /**
     * 从输入流解码图片为内联 BITMAP；不关闭调用方传入的流。
     */
    public TsplLabel image(int x, int y, InputStream in) {
        return image(x, y, in, ImageOptions.defaults());
    }

    public TsplLabel image(int x, int y, InputStream in, ImageOptions options) {
        return append(ImageBitmapRenderer.command(x, y, in, options));
    }

    /**
     * 从图片地址加载：http(s)/file URL，或本地文件路径。
     */
    public TsplLabel image(int x, int y, String location) {
        return image(x, y, location, ImageOptions.defaults());
    }

    public TsplLabel image(int x, int y, String location, ImageOptions options) {
        return append(ImageBitmapRenderer.command(x, y, location, options));
    }

    public TsplLabel image(int x, int y, URL url) {
        return image(x, y, url, ImageOptions.defaults());
    }

    public TsplLabel image(int x, int y, URL url, ImageOptions options) {
        return append(ImageBitmapRenderer.command(x, y, url, options));
    }

    public TsplLabel image(int x, int y, Path path) {
        return image(x, y, path, ImageOptions.defaults());
    }

    public TsplLabel image(int x, int y, Path path, ImageOptions options) {
        return append(ImageBitmapRenderer.command(x, y, path, options));
    }

    public TsplLabel bitmap(int x, int y, int widthBytes, int height, int mode, byte[] data) {
        return append(TsplCmds.bitmap(x, y, widthBytes, height, mode, data));
    }

    public TsplLabel downloadFile(String filename, byte[] fileData) {
        return append(TsplCmds.downloadFile(filename, fileData));
    }

    public TsplLabel print(int sets, int copies) {
        return append(TsplCmds.print(sets, copies));
    }

    public TsplLabel print(int sets) {
        return print(sets, 1);
    }

    /**
     * 常见页头，对齐典型 BarTender / WMS 托盘标签写法。
     */
    public TsplLabel standardHeaderMm(double widthMm, double heightMm) {
        return sizeMm(widthMm, heightMm)
                .direction(0, 0)
                .reference(0, 0)
                .offsetMm(0)
                .peel(false)
                .cutter(false)
                .partialCutter(false)
                .tear(true)
                .cls();
    }

    public TsplDocument build() {
        return TsplDocument.builder()
                .charset(charset)
                .dpi(dpi)
                .addAll(commands)
                .build();
    }

    public byte[] toBytes() {
        return TsplEncoder.encode(build());
    }

    public String toBase64() {
        return TsplEncoder.encodeBase64(build());
    }
}
