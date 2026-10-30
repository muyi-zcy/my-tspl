package tech.muyi.tspl.preview;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.Test;
import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.builder.QrOptions;
import tech.muyi.tspl.builder.TsplLabel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 真实二维码预览渲染。
 */
public class QrPreviewRendererTest {

    @Test
    public void paintsFromTsplCommandLine() {
        DotMatrixCanvas canvas = new DotMatrixCanvas(160, 160);
        assertTrue(QrPreviewRenderer.paint(
                "QRCODE 8,8,L,4,A,0,M2,\"HELLO\"", canvas));
        assertTrue(canvas.get(8, 8));
        assertFalse(canvas.get(8 + 4, 8 + 4));
    }

    @Test
    public void cellWidthScalesModules() throws Exception {
        int modules = QrPreviewRenderer.moduleCount("AB", ErrorCorrectionLevel.L);
        DotMatrixCanvas c2 = new DotMatrixCanvas(400, 400);
        DotMatrixCanvas c4 = new DotMatrixCanvas(400, 400);
        QrPreviewRenderer.stamp(c2, 0, 0, "AB", ErrorCorrectionLevel.L, 2, 0);
        QrPreviewRenderer.stamp(c4, 0, 0, "AB", ErrorCorrectionLevel.L, 4, 0);
        // 定位符白环模块 (1,1)：cell=2 → (2,2)；cell=4 → (4,4)
        assertFalse(c2.get(2, 2));
        assertFalse(c4.get(4, 4));
        // cell=4 时符号占用更大：最右模块列起点 4*(n-1)，cell=2 时该点超出符号
        int edge4 = 4 * (modules - 1);
        assertTrue(edge4 < 400);
        // 顶行定位符模块 0..6 为黑，取模块 0 放大后的末像素
        assertTrue(c4.get(3, 0));
        assertTrue(c2.get(1, 0));
        assertFalse(c2.get(edge4, 0));
    }

    @Test
    public void rotation90MovesTopLeftFinder() throws Exception {
        // 未旋转：左上 (0,0) 黑；90° 后原左上定位符转到右上，左上角仍是某定位符外角
        DotMatrixCanvas canvas = new DotMatrixCanvas(200, 200);
        QrPreviewRenderer.stamp(canvas, 0, 0, "ROT", ErrorCorrectionLevel.M, 3, 90);
        assertTrue(canvas.get(0, 0));
        // 构建指令路径也应能画
        TsplDocument doc = TsplLabel.create()
                .qrcode(0, 0, QrOptions.of("M", 3).rotation(90), "ROT")
                .build();
        assertTrue(QrPreviewRenderer.paint(doc.toCommandLines().get(0), new DotMatrixCanvas(200, 200)));
    }

    @Test
    public void invalidLineReturnsFalse() {
        assertFalse(QrPreviewRenderer.paint("BOX 0,0,10,10,1", new DotMatrixCanvas(16, 16)));
        assertFalse(QrPreviewRenderer.paint("QRCODE 0,0,L,4,A,0", new DotMatrixCanvas(16, 16)));
    }

    @Test
    public void moduleCountIsOddSquareSide() throws Exception {
        int n = QrPreviewRenderer.moduleCount("version-check", ErrorCorrectionLevel.L);
        assertTrue(n >= 21);
        assertEquals(1, n % 4); // 21,25,29... = 21+4k
    }
}
