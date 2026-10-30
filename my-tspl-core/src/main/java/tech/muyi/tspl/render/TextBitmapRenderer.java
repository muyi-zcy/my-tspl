package tech.muyi.tspl.render;

import tech.muyi.tspl.cmd.BitmapCmd;
import tech.muyi.tspl.cmd.TsplCmd;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将文本渲染为内联 TSPL BITMAP 指令（原始单色字节，非十六进制）。
 * <p>
 * TSPL 约定：宽度以字节计；bit0=黑（打印）/ bit1=白；每行高位在前（MSB first）。
 */
public final class TextBitmapRenderer {

    private static final String[] FONT_CANDIDATES = {
            "Microsoft YaHei",
            "PingFang SC",
            "Noto Sans CJK SC",
            "WenQuanYi Micro Hei",
            "SimHei",
            "Dialog"
    };

    private static final ConcurrentHashMap<String, Font> FONT_CACHE = new ConcurrentHashMap<>();

    private static volatile Font baseFont;

    private TextBitmapRenderer() {
    }

    public static TsplCmd command(int x, int y, String text, TextStyle style) {
        return build(x, y, text, style, false, 0);
    }

    public static TsplCmd centered(int y, String text, TextStyle style, int labelWidthDots) {
        return build(0, y, text, style, true, labelWidthDots);
    }

    public static byte[] toBytes(int x, int y, String text, TextStyle style) {
        TsplCmd cmd = command(x, y, text, style);
        tech.muyi.tspl.io.TsplOutput out = new tech.muyi.tspl.io.TsplOutput();
        cmd.writeTo(out);
        return out.toByteArray();
    }

    private static TsplCmd build(int x, int y, String text, TextStyle style,
                                 boolean center, int labelWidth) {
        if (text == null || text.isEmpty()) {
            return out -> {
            };
        }
        TextStyle s = style == null ? TextStyle.bold(24) : style;
        Font font = fontOf(s.fontSize(), s.bold());
        if (s.maxWidth() > 0) {
            font = fitFont(text, font, s.maxWidth(), s.bold());
        }
        BufferedImage image = renderText(text, font);
        int posX = x;
        if (center && labelWidth > 0) {
            posX = Math.max(0, (labelWidth - image.getWidth()) / 2);
        }
        byte[] packed = MonoBitmap.pack(image);
        int widthBytes = MonoBitmap.widthBytes(image.getWidth());
        return new BitmapCmd(posX, y, widthBytes, image.getHeight(), s.bitmapMode(), packed);
    }

    private static Font fitFont(String text, Font start, int maxWidth, boolean bold) {
        Font font = start;
        for (int i = 0; i < 40; i++) {
            int w = measureWidth(text, font);
            if (w <= maxWidth) {
                return font;
            }
            int next = Math.max(8, (int) (font.getSize2D() * maxWidth / (double) w) - 1);
            if (next >= font.getSize()) {
                next = font.getSize() - 2;
            }
            if (next < 8) {
                return fontOf(8, bold);
            }
            font = fontOf(next, bold);
        }
        return font;
    }

    private static int measureWidth(String text, Font font) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = probe.createGraphics();
        g.setFont(font);
        int w = g.getFontMetrics().stringWidth(text);
        g.dispose();
        int padX = Math.max(2, font.getSize() / 16);
        return w + padX * 2;
    }

    private static Font fontOf(int fontSize, boolean bold) {
        String key = fontSize + "|" + bold;
        return FONT_CACHE.computeIfAbsent(key, k -> {
            int style = bold ? Font.BOLD : Font.PLAIN;
            return resolveBaseFont().deriveFont(style, (float) fontSize);
        });
    }

    private static Font resolveBaseFont() {
        if (baseFont != null) {
            return baseFont;
        }
        synchronized (TextBitmapRenderer.class) {
            if (baseFont != null) {
                return baseFont;
            }
            Font fromClasspath = loadClasspathFont();
            if (fromClasspath != null) {
                baseFont = fromClasspath;
                return baseFont;
            }
            for (String name : FONT_CANDIDATES) {
                Font f = new Font(name, Font.BOLD, 12);
                if (!"Dialog".equals(name) && f.getFamily().equalsIgnoreCase("Dialog")) {
                    continue;
                }
                if (f.canDisplay('铁') && f.canDisplay('托')) {
                    baseFont = f;
                    return baseFont;
                }
            }
            baseFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
            return baseFont;
        }
    }

    private static Font loadClasspathFont() {
        String[] paths = {"tspl/label_font.ttf", "tspl/label_font.otf", "tsc/label_font.ttf", "tsc/label_font.otf"};
        for (String path : paths) {
            try (InputStream in = TextBitmapRenderer.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    continue;
                }
                return Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (Exception ignored) {
                // 尝试下一个字体路径
            }
        }
        return null;
    }

    private static BufferedImage renderText(String text, Font font) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g0 = probe.createGraphics();
        g0.setFont(font);
        FontMetrics fm = g0.getFontMetrics();
        int textW = Math.max(1, fm.stringWidth(text));
        int textH = Math.max(1, fm.getAscent() + fm.getDescent());
        g0.dispose();

        int padX = Math.max(2, font.getSize() / 16);
        int padY = Math.max(2, font.getSize() / 16);
        int width = ((textW + padX * 2 + 7) / 8) * 8;
        int height = textH + padY * 2;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            g.setFont(font);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g.drawString(text, padX, padY + fm.getAscent());
        } finally {
            g.dispose();
        }
        return image;
    }

}
