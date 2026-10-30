package tech.muyi.tspl.preview;

import org.junit.Test;
import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.builder.ImageOptions;
import tech.muyi.tspl.builder.QrOptions;
import tech.muyi.tspl.builder.TsplLabel;
import tech.muyi.tspl.cmd.BitmapCmd;
import tech.muyi.tspl.render.MonoBitmap;
import tech.muyi.tspl.render.TextStyle;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * PNG 点阵预览用例。
 */
public class TsplPreviewTest {

    @Test
    public void sizeMmResolvesCanvasAt8DotsPerMm() {
        TsplDocument doc = TsplLabel.create()
                .sizeMm(10, 5)
                .cls()
                .build();
        int[] size = TsplPreview.resolveSize(doc);
        assertEquals(80, size[0]);
        assertEquals(40, size[1]);
    }

    @Test
    public void boxAndLineAppearOnCanvas() {
        TsplDocument doc = TsplLabel.create()
                .cls()
                .box(1, 1, 10, 6, 1)
                .hline(1, 3, 9, 1)
                .build();

        DotMatrixCanvas canvas = TsplPreview.render(doc, 12, 8);
        assertTrue(canvas.get(1, 1));
        assertTrue(canvas.get(10, 1));
        assertTrue(canvas.get(1, 6));
        assertTrue(canvas.get(5, 3));
    }

    @Test
    public void solidBlackBitmapIsFullyInk() {
        byte[] data = new byte[1 * 4];
        BitmapCmd cmd = new BitmapCmd(0, 0, 1, 4, 1, data);
        DotMatrixCanvas canvas = TsplPreview.render(cmd);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 8; x++) {
                assertTrue(canvas.get(x, y));
            }
        }
    }

    @Test
    public void checkerPatternUsesMsbFirstPolarity() {
        byte[] data = new byte[]{(byte) 0x55};
        BitmapCmd cmd = new BitmapCmd(0, 0, 1, 1, 0, data);
        DotMatrixCanvas canvas = TsplPreview.render(cmd);
        assertTrue(MonoBitmap.isBlack(data, 1, 0, 0));
        assertFalse(MonoBitmap.isBlack(data, 1, 1, 0));
        assertTrue(canvas.get(0, 0));
        assertFalse(canvas.get(1, 0));
    }

    @Test
    public void orModeCompositesOverlappingBitmaps() {
        DotMatrixCanvas canvas = new DotMatrixCanvas(16, 8);
        canvas.stamp(new BitmapCmd(0, 0, 1, 8, 1, new byte[8]));
        canvas.stamp(new BitmapCmd(8, 0, 1, 8, 1, new byte[8]));
        assertTrue(canvas.get(0, 0));
        assertTrue(canvas.get(8, 0));
        assertTrue(canvas.get(15, 7));
    }

    @Test
    public void solidImageBitmapLooksFilled() throws Exception {
        byte[] png = solidPng(16, 8, Color.BLACK);
        TsplDocument doc = TsplLabel.create()
                .cls()
                .image(0, 0, png, ImageOptions.maxWidth(16))
                .build();

        BitmapCmd bitmap = firstBitmap(doc);
        Path out = TsplPreview.toPng(bitmap, PreviewPaths.file("solid-16x8.png"),
                ImagePreviewOptions.defaults().zoom(8).roundDots(false).padding(0));
        assertTrue(Files.size(out) > 50);

        DotMatrixCanvas canvas = TsplPreview.render(bitmap);
        assertEquals(16, canvas.width());
        assertEquals(8, canvas.height());
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                assertTrue(canvas.get(x, y));
            }
        }
    }

    @Test
    public void textBitmapWritesPng() throws Exception {
        TsplDocument doc = TsplLabel.create()
                .cls()
                .textBitmap(0, 0, "A", TextStyle.bold(32))
                .build();
        BitmapCmd bitmap = firstBitmap(doc);
        Path out = TsplPreview.toPng(bitmap, PreviewPaths.file("text-A.png"),
                ImagePreviewOptions.defaults().zoom(4).roundDots(false));
        assertTrue(Files.size(out) > 100);
        System.out.println("wrote " + out.toAbsolutePath());

        DotMatrixCanvas canvas = TsplPreview.render(bitmap);
        boolean anyBlack = false;
        for (int y = 0; y < canvas.height() && !anyBlack; y++) {
            for (int x = 0; x < canvas.width(); x++) {
                if (canvas.get(x, y)) {
                    anyBlack = true;
                    break;
                }
            }
        }
        assertTrue(anyBlack);
    }

    @Test
    public void fullLabelWritesPng() throws Exception {
        byte[] logo = solidPng(32, 16, Color.DARK_GRAY);
        TsplDocument doc = TsplLabel.create()
                .standardHeaderMm(40, 30)
                .box(8, 8, 300, 220, 2)
                .hline(8, 40, 292, 1)
                .textBitmap(16, 12, "DEMO", TextStyle.bold(28))
                .image(16, 50, logo, ImageOptions.maxWidth(64))
                .qrcode(220, 50, QrOptions.of("L", 4), "TRAY-001")
                .print(1)
                .build();

        Path png = TsplPreview.toPng(doc, PreviewPaths.file("full-label.png"),
                ImagePreviewOptions.defaults().zoom(3).roundDots(true));
        assertTrue(Files.size(png) > 500);
        System.out.println("wrote " + png.toAbsolutePath());

        DotMatrixCanvas canvas = TsplPreview.render(doc);
        assertEquals(320, canvas.width());
        assertEquals(240, canvas.height());
        assertTrue(canvas.get(8, 8));
        assertTrue(canvas.get(300, 8));

        BufferedImage image = TsplPreview.toImage(doc, ImagePreviewOptions.defaults().zoom(2));
        assertEquals(320 * 2 + 8, image.getWidth());
        assertEquals(240 * 2 + 8, image.getHeight());
    }

    @Test
    public void qrcodeRendersRealFinderPattern() {
        // 默认 cellWidth=6；左上 7×7 定位符特征：外角黑、内环白、中心黑
        TsplDocument doc = TsplLabel.create()
                .cls()
                .qrcode(10, 10, "X")
                .build();
        DotMatrixCanvas canvas = TsplPreview.render(doc, 200, 200);
        assertTrue(canvas.get(10, 10));
        // 定位符中心黑块模块 (3,3)
        assertTrue(canvas.get(10 + 18, 10 + 18));
        // 白环模块 (1,1) / (1,3)
        assertFalse(canvas.get(10 + 6, 10 + 6));
        assertFalse(canvas.get(10 + 6, 10 + 18));
    }

    @Test
    public void qrcodeWritesDecodablePng() throws Exception {
        TsplDocument doc = TsplLabel.create()
                .cls()
                .qrcode(0, 0, QrOptions.of("M", 4), "TRAY-DECODE-OK")
                .build();
        Path out = TsplPreview.toPng(doc, PreviewPaths.file("qr-real.png"),
                ImagePreviewOptions.defaults().zoom(3).roundDots(false).padding(0));
        assertTrue(Files.size(out) > 200);
        System.out.println("wrote " + out.toAbsolutePath());

        DotMatrixCanvas canvas = TsplPreview.render(doc, 200, 200);
        int black = 0;
        for (int y = 0; y < canvas.height(); y++) {
            for (int x = 0; x < canvas.width(); x++) {
                if (canvas.get(x, y)) {
                    black++;
                }
            }
        }
        // 真实二维码应有大量墨点，远超占位框描边
        assertTrue(black > 200);
    }

    @Test
    public void pngRoundDotsDifferFromPaper() {
        DotMatrixCanvas canvas = new DotMatrixCanvas(4, 4);
        canvas.set(1, 1, true);
        BufferedImage image = canvas.toImage(ImagePreviewOptions.defaults().zoom(5).padding(0));
        assertEquals(20, image.getWidth());
        assertEquals(20, image.getHeight());
        int inkSample = image.getRGB(5 + 2, 5 + 2) & 0xFFFFFF;
        int paper = ImagePreviewOptions.defaults().paper().getRGB() & 0xFFFFFF;
        assertTrue(inkSample != paper);
    }

    private static BitmapCmd firstBitmap(TsplDocument doc) {
        for (Object cmd : doc.commands()) {
            if (cmd instanceof BitmapCmd) {
                return (BitmapCmd) cmd;
            }
        }
        throw new AssertionError("no BitmapCmd");
    }

    private static byte[] solidPng(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
