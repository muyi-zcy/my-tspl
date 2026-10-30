package tech.muyi.tspl.render;

/**
 * 服务端文本 → BITMAP 渲染的样式。
 */
public final class TextStyle {

    private final int fontSize;
    private final boolean bold;
    private final int maxWidth;
    private final int bitmapMode;

    private TextStyle(int fontSize, boolean bold, int maxWidth, int bitmapMode) {
        this.fontSize = Math.max(8, fontSize);
        this.bold = bold;
        this.maxWidth = Math.max(0, maxWidth);
        this.bitmapMode = bitmapMode;
    }

    public static TextStyle of(int fontSize) {
        return new TextStyle(fontSize, true, 0, 1);
    }

    public static TextStyle bold(int fontSize) {
        return new TextStyle(fontSize, true, 0, 1);
    }

    public static TextStyle plain(int fontSize) {
        return new TextStyle(fontSize, false, 0, 1);
    }

    public TextStyle maxWidth(int maxWidthDots) {
        return new TextStyle(fontSize, bold, maxWidthDots, bitmapMode);
    }

    /**
     * BITMAP 模式：0=覆盖（OVERWRITE），1=或（OR），2=异或（XOR）。
     */
    public TextStyle bitmapMode(int mode) {
        return new TextStyle(fontSize, bold, maxWidth, mode);
    }

    public int fontSize() {
        return fontSize;
    }

    public boolean bold() {
        return bold;
    }

    public int maxWidth() {
        return maxWidth;
    }

    public int bitmapMode() {
        return bitmapMode;
    }
}
