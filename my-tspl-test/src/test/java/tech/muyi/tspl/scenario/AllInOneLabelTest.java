package tech.muyi.tspl.scenario;

import org.junit.Test;
import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.TsplDpi;
import tech.muyi.tspl.builder.ImageOptions;
import tech.muyi.tspl.builder.QrOptions;
import tech.muyi.tspl.builder.TsplLabel;
import tech.muyi.tspl.preview.DotMatrixCanvas;
import tech.muyi.tspl.preview.ImagePreviewOptions;
import tech.muyi.tspl.preview.PreviewPaths;
import tech.muyi.tspl.preview.TsplPreview;
import tech.muyi.tspl.render.TextStyle;
import tech.muyi.tspl.sink.TsplClient;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 单张 10cm×10cm（100mm×100mm）综合样张。
 * <p>
 * 通过 {@link TsplLabel#dpi(int)} 声明打印机分辨率；坐标用 {@code label.dots(...)} /
 * {@code label.mm(...)} 换算，避免 203 坐标打到 300dpi 机只占约 2/3。
 * <p>
 * PNG：{@code target/preview/styles/00-all-in-one-10cm.png}
 * <p>
 * 实机：填 {@link #PRINTER_HOST} 或 {@code -Dtspl.printer.host=}；留空则只预览。
 */
public class AllInOneLabelTest {

    private static final Path PIC = Paths.get("src/test/resources/fixtures/pic.jpg");

    /**
     * 打印机 DPI。常见热敏：203 或 300。也可用 {@code -Dtspl.printer.dpi=203}。
     */
    private static final int PRINTER_DPI = 300;

    /**
     * 打印机 IP，留空跳过实机下发。也可 {@code -Dtspl.printer.host=} / {@code TSPL_PRINTER_HOST}。
     */
    private static final String PRINTER_HOST = "";
    private static final int PRINTER_PORT = 9100;
    private static final int PRINTER_TIMEOUT_MS = 3000;

    private static final ImagePreviewOptions PREVIEW =
            ImagePreviewOptions.defaults().zoom(2).roundDots(true);

    @Test
    public void allStylesOnOne_10cmLabel() throws Exception {
        assertTrue("fixture missing: " + PIC.toAbsolutePath(), Files.isRegularFile(PIC));

        int dpiValue = resolveDpiValue();
        TsplDocument doc = buildAllInOneLabel(dpiValue);
        assertEquals(dpiValue >= 280 ? TsplDpi.DPI_300 : TsplDpi.DPI_203, doc.dpi().dpi());

        int dots = doc.dpi().mm(100);

        List<String> lines = doc.toCommandLines();
        assertTrue(lines.stream().anyMatch(l -> l.contains("SIZE 100 mm, 100 mm")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("BOX ")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("LINE ")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("BITMAP ")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("QRCODE ")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("BARCODE ")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("TEXT ")));
        assertTrue(lines.contains("PRINT 1,1"));

        // 预览自动读 doc.dpi()
        int[] size = TsplPreview.resolveSize(doc);
        assertEquals(dots, size[0]);
        assertEquals(dots, size[1]);

        DotMatrixCanvas canvas = TsplPreview.render(doc);
        assertEquals(dots, canvas.width());
        assertEquals(dots, canvas.height());
        assertTrue(canvas.get(doc.dpi().dots(16), doc.dpi().dots(16)));
        assertTrue(canvas.get(doc.dpi().dots(784), doc.dpi().dots(16)));
        assertTrue(canvas.get(doc.dpi().dots(420), doc.dpi().dots(270)));

        Path out = TsplPreview.toPng(doc, PreviewPaths.file("styles/00-all-in-one-10cm.png"), PREVIEW);
        assertTrue(Files.exists(out));
        assertTrue(Files.size(out) > 2000);
        System.out.println("all-in-one 10cm @ " + doc.dpi() + " -> " + out.toAbsolutePath());

        maybeSendToPrinter(doc);
    }

    /**
     * 版面按 203dpi 点坐标设计，经 {@link TsplLabel#dots(int)} 换算到目标 DPI。
     */
    static TsplDocument buildAllInOneLabel(int dpiValue) throws Exception {
        TsplLabel label = TsplLabel.create().dpi(dpiValue);
        byte[] logo = logoPng(label.dots(72), label.dots(36));
        byte[] gradient = gradientPng(label.dots(100), label.dots(40));

        return label
                .standardHeaderMm(100, 100)
                .speed(4)
                .density(8)

                .box(label.dots(16), label.dots(16), label.dots(784), label.dots(784), 3)

                .textBitmap(label.dots(28), label.dots(28), "综合样张 10×10cm",
                        TextStyle.bold(label.dots(36)))
                .textBitmap(label.dots(28), label.dots(72),
                        "文字·框线·QR·条码·图片·密集字段·机内TEXT",
                        TextStyle.plain(label.dots(16)))
                .image(label.dots(640), label.dots(28), logo, ImageOptions.maxWidth(label.dots(120)))
                .hline(label.dots(16), label.dots(88), label.dots(768), 2)

                .textBitmap(label.dots(28), label.dots(100), "Bold32 标题",
                        TextStyle.bold(label.dots(32)))
                .textBitmap(label.dots(260), label.dots(108), "Plain22 正文",
                        TextStyle.plain(label.dots(22)))
                .textBitmap(label.dots(460), label.dots(112), "小号 Bold16",
                        TextStyle.bold(label.dots(16)))
                .textBitmapCentered(label.dots(150), "居中 Centered",
                        TextStyle.bold(label.dots(28)), label.mm(100))
                .textBitmap(label.dots(28), label.dots(190),
                        "maxWidth：超长文案自动缩小字号以适配本行宽度限制示例",
                        TextStyle.plain(label.dots(20)).maxWidth(label.dots(740)))
                .hline(label.dots(16), label.dots(220), label.dots(768), 2)

                .box(label.dots(28), label.dots(236), label.dots(390), label.dots(444), 2)
                .vline(label.dots(140), label.dots(236), label.dots(208), 2)
                .hline(label.dots(28), label.dots(288), label.dots(362), 1)
                .hline(label.dots(28), label.dots(340), label.dots(362), 1)
                .hline(label.dots(28), label.dots(392), label.dots(362), 1)
                .textBitmap(label.dots(40), label.dots(250), "单号", TextStyle.bold(label.dots(18)))
                .textBitmap(label.dots(152), label.dots(248), "WO-20261030",
                        TextStyle.plain(label.dots(22)))
                .textBitmap(label.dots(40), label.dots(302), "客户", TextStyle.bold(label.dots(18)))
                .textBitmap(label.dots(152), label.dots(300), "华东仓",
                        TextStyle.plain(label.dots(22)))
                .textBitmap(label.dots(40), label.dots(354), "品名", TextStyle.bold(label.dots(18)))
                .textBitmap(label.dots(152), label.dots(352), "包装纸箱",
                        TextStyle.plain(label.dots(22)))
                .textBitmap(label.dots(40), label.dots(406), "数量", TextStyle.bold(label.dots(18)))
                .textBitmap(label.dots(152), label.dots(404), "120 件",
                        TextStyle.plain(label.dots(22)))
                .line(label.dots(300), label.dots(400), label.dots(380), label.dots(440), 1)

                .textBitmap(label.dots(420), label.dots(240), "扫码区 QR + Code128",
                        TextStyle.bold(label.dots(18)))
                .qrcode(label.dots(420), label.dots(270),
                        QrOptions.of("M", label.dots(5)), "ALL-IN-ONE-10CM")
                .qrcode(label.dots(620), label.dots(270),
                        QrOptions.of("L", label.dots(4)).rotation(90), "ROT90")
                .barcode(label.dots(420), label.dots(400), "128",
                        label.dots(36), 1, 0, 2, 4, "SKU77881234")

                .hline(label.dots(16), label.dots(460), label.dots(768), 2)

                .textBitmap(label.dots(28), label.dots(472), "threshold",
                        TextStyle.bold(label.dots(16)))
                .image(label.dots(28), label.dots(496), gradient,
                        ImageOptions.maxWidth(label.dots(160)).threshold(100))
                .textBitmap(label.dots(210), label.dots(472), "dither",
                        TextStyle.bold(label.dots(16)))
                .image(label.dots(210), label.dots(496), gradient,
                        ImageOptions.maxWidth(label.dots(160)).dither(true))
                .textBitmap(label.dots(400), label.dots(472), "JPEG dither",
                        TextStyle.bold(label.dots(16)))
                .image(label.dots(400), label.dots(496), PIC,
                        ImageOptions.maxSize(label.dots(200), label.dots(130)).dither(true))
                .textBitmap(label.dots(620), label.dots(472), "stretch",
                        TextStyle.bold(label.dots(16)))
                .image(label.dots(620), label.dots(496), logo,
                        ImageOptions.maxSize(label.dots(140), label.dots(80)).keepAspect(false))

                .hline(label.dots(16), label.dots(640), label.dots(768), 2)

                .textBitmap(label.dots(28), label.dots(652),
                        "密集: SKU=ABC-1001  规格=200pcs  质检=OK  防潮存放",
                        TextStyle.plain(label.dots(16)).maxWidth(label.dots(500)))
                .textBitmap(label.dots(28), label.dots(686),
                        "托盘 A1-8899  库位 A-12-03  钢板 Q235 2.0mm",
                        TextStyle.plain(label.dots(16)).maxWidth(label.dots(500)))
                .text(label.dots(28), label.dots(730), "0", 0, 1, 1, "NATIVE TEXT FONT0")
                .text(label.dots(280), label.dots(720), "2", 0, 2, 2, "MUL2x2")
                .textBitmap(label.dots(520), label.dots(700), "BITMAP混排",
                        TextStyle.bold(label.dots(22)))
                .qrcode(label.dots(680), label.dots(660),
                        QrOptions.of("L", label.dots(3)), "FOOT")

                .print(1, 1)
                .build();
    }

    private static void maybeSendToPrinter(TsplDocument doc) {
        String host = resolvePrinterHost();
        if (host.isEmpty()) {
            System.out.println("printer host empty — skip live print "
                    + "(set PRINTER_HOST or -Dtspl.printer.host=)");
            return;
        }
        int port = resolvePrinterPort();
        try (TsplClient client = TsplClient.printer(host, port, PRINTER_TIMEOUT_MS)) {
            client.send(doc);
        }
        System.out.println("sent all-in-one label to printer " + host + ":" + port
                + " @ " + doc.dpi());
    }

    private static int resolveDpiValue() {
        String prop = System.getProperty("tspl.printer.dpi");
        if (prop != null && !prop.trim().isEmpty()) {
            return Integer.parseInt(prop.trim());
        }
        String env = System.getenv("TSPL_PRINTER_DPI");
        if (env != null && !env.trim().isEmpty()) {
            return Integer.parseInt(env.trim());
        }
        return PRINTER_DPI;
    }

    private static String resolvePrinterHost() {
        String prop = System.getProperty("tspl.printer.host");
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }
        String env = System.getenv("TSPL_PRINTER_HOST");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        return PRINTER_HOST == null ? "" : PRINTER_HOST.trim();
    }

    private static int resolvePrinterPort() {
        String prop = System.getProperty("tspl.printer.port");
        if (prop != null && !prop.trim().isEmpty()) {
            return Integer.parseInt(prop.trim());
        }
        String env = System.getenv("TSPL_PRINTER_PORT");
        if (env != null && !env.trim().isEmpty()) {
            return Integer.parseInt(env.trim());
        }
        return PRINTER_PORT;
    }

    private static byte[] gradientPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = (x * 255) / Math.max(1, width - 1);
                image.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return toPngBytes(image);
    }

    private static byte[] logoPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            g.fillRoundRect(4, 4, width - 8, height - 8, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, height / 2)));
            g.drawString("LOGO", 12, height - height / 4);
        } finally {
            g.dispose();
        }
        return toPngBytes(image);
    }

    private static byte[] toPngBytes(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
