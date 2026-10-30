package tech.muyi.tspl;

import org.junit.Test;
import tech.muyi.tspl.builder.ImageOptions;
import tech.muyi.tspl.builder.TsplLabel;
import tech.muyi.tspl.preview.ImagePreviewOptions;
import tech.muyi.tspl.preview.PreviewPaths;
import tech.muyi.tspl.preview.TsplPreview;
import tech.muyi.tspl.render.TextStyle;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * 使用真实 JPEG {@code src/test/resources/fixtures/pic.jpg} 验证路径 / 流入图。
 */
public class PicJpgImageTest {

    private static final Path PIC = Paths.get("src/test/resources/fixtures/pic.jpg");

    @Test
    public void imageFromPathAndStream() throws Exception {
        assertTrue("fixture missing: " + PIC.toAbsolutePath(), Files.isRegularFile(PIC));

        byte[] fromPath = TsplLabel.create()
                .standardHeaderMm(100, 80)
                .image(40, 40, PIC, ImageOptions.maxWidth(400))
                .print(1)
                .toBytes();

        byte[] fromLocation;
        try (InputStream in = Files.newInputStream(PIC)) {
            fromLocation = TsplLabel.create()
                    .standardHeaderMm(100, 80)
                    .image(40, 40, in, ImageOptions.maxWidth(400))
                    .print(1)
                    .toBytes();
        }

        byte[] fromString = TsplLabel.create()
                .standardHeaderMm(100, 80)
                .image(40, 40, PIC.toString(), ImageOptions.maxWidth(400))
                .print(1)
                .toBytes();

        String expected = "BITMAP 40,40,";
        assertTrue(new String(fromPath, Charset.forName("ISO-8859-1")).contains(expected));
        assertTrue(new String(fromLocation, Charset.forName("ISO-8859-1")).contains(expected));
        assertTrue(new String(fromString, Charset.forName("ISO-8859-1")).contains(expected));
        assertTrue(fromPath.length > 1000);
        assertTrue(fromPath.length == fromLocation.length);
        assertTrue(fromPath.length == fromString.length);
    }

    @Test
    public void previewLabelWithPicJpg() throws Exception {
        assertTrue("fixture missing: " + PIC.toAbsolutePath(), Files.isRegularFile(PIC));

        TsplDocument doc = TsplLabel.create()
                .standardHeaderMm(100, 80)
                .box(20, 20, 780, 600, 2)
                .textBitmap(40, 36, "真实 JPEG 入图", TextStyle.bold(28))
                .image(40, 90, PIC, ImageOptions.maxWidth(320).dither(true))
                .print(1)
                .build();

        Path out = TsplPreview.toPng(doc, PreviewPaths.file("pic-jpg-label.png"),
                ImagePreviewOptions.defaults().zoom(2).roundDots(true));
        assertTrue(Files.exists(out));
        assertTrue(Files.size(out) > 500);
        System.out.println("pic.jpg preview -> " + out.toAbsolutePath());
    }
}
