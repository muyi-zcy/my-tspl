package tech.muyi.tspl;

import org.junit.Test;
import tech.muyi.tspl.builder.ImageOptions;
import tech.muyi.tspl.builder.QrOptions;
import tech.muyi.tspl.builder.TsplLabel;
import tech.muyi.tspl.encode.TsplEncoder;
import tech.muyi.tspl.render.TextStyle;
import tech.muyi.tspl.sink.Base64CollectingSink;
import tech.muyi.tspl.sink.TsplClient;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link TsplLabel} Builder、文档输出与 Base64 Sink 相关用例。
 */
public class TsplLabelTest {

    /**
     * 标准页头 + 二维码 + PRINT 应生成期望的文本指令（SIZE / TEAR / QRCODE / PRINT，且为 CRLF）。
     */
    @Test
    public void builderProducesExpectedTextCommands() {
        byte[] bytes = TsplLabel.create()
                .standardHeaderMm(100, 100)
                .qrcode(680, 110, QrOptions.of("L", 20), "B3-9999")
                .print(1, 1)
                .toBytes();

        String text = new String(bytes, TsplCharsets.GB2312);
        assertTrue(text.contains("SIZE 100 mm, 100 mm"));
        assertTrue(text.contains("SET TEAR ON"));
        assertTrue(text.contains("QRCODE 680,110,L,20,A,0,M2,\"B3-9999\""));
        assertTrue(text.contains("PRINT 1,1"));
        assertTrue(text.contains("\r\n"));
    }

    /**
     * 图形指令：BOX、水平/垂直线、斜线应正确展开为 BOX / LINE，线宽 0 按 1 处理。
     */
    @Test
    public void graphicsCommandsProduceLineAndBox() {
        byte[] bytes = TsplLabel.create()
                .cls()
                .box(20, 20, 780, 600, 3)
                .hline(20, 100, 760, 2)
                .vline(100, 20, 200, 1)
                .line(0, 0, 10, 10, 0)
                .print(1)
                .toBytes();

        String text = new String(bytes, TsplCharsets.GB2312);
        assertTrue(text.contains("BOX 20,20,780,600,3"));
        assertTrue(text.contains("LINE 20,100,780,100,2"));
        assertTrue(text.contains("LINE 100,20,100,220,1"));
        assertTrue(text.contains("LINE 0,0,10,10,1"));
    }

    /**
     * textBitmap 应插入 BITMAP 指令及二进制位图载荷，而非纯文本 TEXT。
     */
    @Test
    public void textBitmapInsertsBinaryPayload() {
        byte[] bytes = TsplLabel.create()
                .cls()
                .textBitmap(50, 30, "ABC", TextStyle.bold(24))
                .print(1)
                .toBytes();

        String asciiProbe = new String(bytes, Charset.forName("ISO-8859-1"));
        assertTrue(asciiProbe.contains("BITMAP 50,30,"));
        assertTrue(bytes.length > 40);
    }

    /**
     * image 应从 PNG 字节渲染出 BITMAP（含宽高与模式参数），并附带打包后的位图数据。
     */
    @Test
    public void imageInsertsBitmapFromPngBytes() throws Exception {
        byte[] png = solidPng(16, 8, Color.BLACK);
        byte[] bytes = TsplLabel.create()
                .cls()
                .image(40, 120, png, ImageOptions.maxWidth(16))
                .print(1)
                .toBytes();

        String asciiProbe = new String(bytes, Charset.forName("ISO-8859-1"));
        assertTrue(asciiProbe.contains("BITMAP 40,120,2,8,1,"));
        // header + 2*8 packed bytes + CRLF + surrounding cmds
        assertTrue(bytes.length > 30);
    }

    /**
     * image 应支持 InputStream / 本地路径 / file URL。
     */
    @Test
    public void imageAcceptsStreamPathAndUrl() throws Exception {
        byte[] png = solidPng(16, 8, Color.BLACK);
        Path tmp = Files.createTempFile("my-tspl-logo-", ".png");
        try {
            Files.write(tmp, png);

            byte[] fromStream = TsplLabel.create()
                    .cls()
                    .image(10, 20, new ByteArrayInputStream(png), ImageOptions.maxWidth(16))
                    .print(1)
                    .toBytes();
            byte[] fromPath = TsplLabel.create()
                    .cls()
                    .image(10, 20, tmp, ImageOptions.maxWidth(16))
                    .print(1)
                    .toBytes();
            byte[] fromUrl = TsplLabel.create()
                    .cls()
                    .image(10, 20, tmp.toUri().toURL(), ImageOptions.maxWidth(16))
                    .print(1)
                    .toBytes();
            byte[] fromLocation = TsplLabel.create()
                    .cls()
                    .image(10, 20, tmp.toString(), ImageOptions.maxWidth(16))
                    .print(1)
                    .toBytes();

            String expected = "BITMAP 10,20,2,8,1,";
            assertTrue(new String(fromStream, Charset.forName("ISO-8859-1")).contains(expected));
            assertTrue(new String(fromPath, Charset.forName("ISO-8859-1")).contains(expected));
            assertTrue(new String(fromUrl, Charset.forName("ISO-8859-1")).contains(expected));
            assertTrue(new String(fromLocation, Charset.forName("ISO-8859-1")).contains(expected));
            assertEquals(fromPath.length, fromUrl.length);
            assertEquals(fromPath.length, fromLocation.length);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * 综合标签：页头、框线、中文位图文字、图片、二维码与 PRINT 应同时出现在输出中。
     */
    @Test
    public void fullLabelWithCommonElements() throws Exception {
        byte[] logo = solidPng(32, 16, Color.DARK_GRAY);
        byte[] bytes = TsplLabel.create()
                .standardHeaderMm(100, 80)
                .box(20, 20, 780, 600, 3)
                .hline(20, 100, 760, 2)
                .textBitmap(40, 40, "铁托盘", TextStyle.bold(24))
                .image(40, 120, logo, ImageOptions.maxWidth(200))
                .qrcode(560, 120, QrOptions.of("L", 8), "TRAY-001")
                .print(1)
                .toBytes();

        String asciiProbe = new String(bytes, Charset.forName("ISO-8859-1"));
        assertTrue(asciiProbe.contains("BOX 20,20,780,600,3"));
        assertTrue(asciiProbe.contains("LINE 20,100,780,100,2"));
        assertTrue(asciiProbe.contains("BITMAP "));
        assertTrue(asciiProbe.contains("QRCODE 560,120,L,8,A,0,M2,\"TRAY-001\""));
        assertTrue(asciiProbe.contains("PRINT 1,1"));
    }

    /**
     * toCommandLines / toCommands 应输出可读文本行；BITMAP 以摘要形式展示，不含原始二进制空字节。
     */
    @Test
    public void documentToCommandsOutputsReadableLines() {
        TsplDocument doc = TsplLabel.create()
                .cls()
                .box(10, 10, 100, 80, 2)
                .textBitmap(50, 30, "ABC", TextStyle.bold(24))
                .qrcode(10, 10, "CODE")
                .print(1)
                .build();

        List<String> lines = doc.toCommandLines();
        assertTrue(lines.contains("CLS"));
        assertTrue(lines.contains("BOX 10,10,100,80,2"));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("BITMAP 50,30,") && l.contains(" bytes>")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("QRCODE 10,10,")));
        assertTrue(lines.contains("PRINT 1,1"));

        String text = doc.toCommands();
        System.out.println(text);
        assertTrue(text.contains("CLS\r\n"));
        assertTrue(text.contains("BOX 10,10,100,80,2\r\n"));
        assertFalse(text.contains(new String(new byte[]{(byte) 0x00})));
    }

    /**
     * TsplClient.withBase64 每 send 一次文档应收集一条 Base64，内容与编码器结果一致。
     */
    @Test
    public void base64ClientCollectsOneEntryPerDocument() {
        TsplDocument doc = TsplLabel.create().cls().print(1).build();
        List<String> list = TsplClient.withBase64(c -> c.send(doc).send(doc));
        assertEquals(2, list.size());
        assertFalse(list.get(0).isEmpty());
        assertEquals(TsplEncoder.encodeBase64(doc), list.get(0));
    }

    /**
     * Base64CollectingSink.drainBase64 取出后应清空缓存，size 归零。
     */
    @Test
    public void base64SinkDrainClears() {
        Base64CollectingSink sink = new Base64CollectingSink();
        sink.write(new byte[]{1, 2, 3});
        assertEquals(1, sink.drainBase64().size());
        assertEquals(0, sink.size());
        sink.close();
    }

    /**
     * dpi 写入文档元数据；mm/dots 按 DPI 换算（300dpi 下 100mm=1200，203 设计点×1.5）。
     */
    @Test
    public void dpiIsStoredAndConvertsMmDots() {
        TsplLabel label = TsplLabel.create().dpi(300);
        assertEquals(TsplDpi.DPI_300, label.dpi().dpi());
        assertEquals(12, label.dpi().dotsPerMm());
        assertEquals(1200, label.mm(100));
        assertEquals(60, label.dots(40));

        TsplDocument doc = label
                .standardHeaderMm(100, 100)
                .box(label.dots(16), label.dots(16), label.mm(98), label.mm(98), 3)
                .print(1)
                .build();
        assertEquals(TsplDpi.DPI_300, doc.dpi().dpi());
        assertTrue(doc.toCommands().contains("BOX 24,24,1176,1176,3"));
    }

    @Test
    public void defaultDpiIs203() {
        TsplDocument doc = TsplLabel.create().cls().build();
        assertEquals(TsplDpi.DPI_203, doc.dpi().dpi());
        assertEquals(8, doc.dpi().dotsPerMm());
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
