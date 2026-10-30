package tech.muyi.tspl.preview;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 TSPL {@code QRCODE} 指令解析并用 ZXing 渲成真实二维码点阵（按 cellWidth 放大模块）。
 * <p>
 * 支持格式：{@code QRCODE x,y,ECC,cellWidth,mode,rotation[,model,...],"content"}
 */
public final class QrPreviewRenderer {

    /**
     * 捕获 x/y/ECC/cell/mode/rotation；中间可选参数忽略；末尾引号内容。
     */
    private static final Pattern QRCODE = Pattern.compile(
            "^QRCODE\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*([LMQH])\\s*,\\s*(\\d+)\\s*,\\s*([A-Za-z])\\s*,\\s*(\\d+)"
                    + "(?:\\s*,\\s*[^,\"]+)*\\s*,\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*$",
            Pattern.CASE_INSENSITIVE);

    private QrPreviewRenderer() {
    }

    /**
     * @return true 若已识别并绘制；false 表示非 QRCODE 或不完整（调用方可回退占位）
     */
    public static boolean paint(String commandLine, DotMatrixCanvas canvas) {
        if (commandLine == null || canvas == null) {
            return false;
        }
        Matcher m = QRCODE.matcher(commandLine.trim());
        if (!m.matches()) {
            return false;
        }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        ErrorCorrectionLevel ecc = eccOf(m.group(3));
        int cell = Math.max(1, Integer.parseInt(m.group(4)));
        int rotation = Integer.parseInt(m.group(6));
        String content = unescape(m.group(7));
        try {
            stamp(canvas, x, y, content, ecc, cell, rotation);
            return true;
        } catch (WriterException e) {
            return false;
        }
    }

    public static void stamp(DotMatrixCanvas canvas, int x, int y, String content,
                             ErrorCorrectionLevel ecc, int cellWidth, int rotationDeg)
            throws WriterException {
        if (canvas == null || content == null) {
            return;
        }
        boolean[][] modules = encode(content, ecc == null ? ErrorCorrectionLevel.L : ecc);
        modules = rotate(modules, normalizeRotation(rotationDeg));
        int cell = Math.max(1, cellWidth);
        int n = modules.length;
        for (int my = 0; my < n; my++) {
            for (int mx = 0; mx < n; mx++) {
                if (!modules[my][mx]) {
                    continue;
                }
                int px0 = x + mx * cell;
                int py0 = y + my * cell;
                for (int dy = 0; dy < cell; dy++) {
                    for (int dx = 0; dx < cell; dx++) {
                        canvas.set(px0 + dx, py0 + dy, true);
                    }
                }
            }
        }
    }

    /** 模块矩阵边长（未乘 cellWidth）。 */
    public static int moduleCount(String content, ErrorCorrectionLevel ecc) throws WriterException {
        return encode(content, ecc == null ? ErrorCorrectionLevel.L : ecc).length;
    }

    private static boolean[][] encode(String content, ErrorCorrectionLevel ecc) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ecc);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // 打印机 QRCODE 坐标对准符号本体；静区由标签留白承担
        hints.put(EncodeHintType.MARGIN, 0);
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        int n = Math.min(w, h);
        boolean[][] out = new boolean[n][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                out[y][x] = matrix.get(x, y);
            }
        }
        return out;
    }

    /** 顺时针旋转 0/90/180/270。 */
    private static boolean[][] rotate(boolean[][] src, int deg) {
        if (deg == 0) {
            return src;
        }
        int n = src.length;
        boolean[][] dst = new boolean[n][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (!src[y][x]) {
                    continue;
                }
                int nx;
                int ny;
                if (deg == 90) {
                    nx = n - 1 - y;
                    ny = x;
                } else if (deg == 180) {
                    nx = n - 1 - x;
                    ny = n - 1 - y;
                } else { // 270
                    nx = y;
                    ny = n - 1 - x;
                }
                dst[ny][nx] = true;
            }
        }
        return dst;
    }

    private static int normalizeRotation(int deg) {
        int d = deg % 360;
        if (d < 0) {
            d += 360;
        }
        if (d <= 45 || d > 315) {
            return 0;
        }
        if (d <= 135) {
            return 90;
        }
        if (d <= 225) {
            return 180;
        }
        return 270;
    }

    private static ErrorCorrectionLevel eccOf(String ecc) {
        if (ecc == null || ecc.isEmpty()) {
            return ErrorCorrectionLevel.L;
        }
        switch (Character.toUpperCase(ecc.charAt(0))) {
            case 'M':
                return ErrorCorrectionLevel.M;
            case 'Q':
                return ErrorCorrectionLevel.Q;
            case 'H':
                return ErrorCorrectionLevel.H;
            case 'L':
            default:
                return ErrorCorrectionLevel.L;
        }
    }

    private static String unescape(String s) {
        if (s == null || s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                sb.append(s.charAt(++i));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
