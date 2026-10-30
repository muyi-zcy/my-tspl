package tech.muyi.tspl.render;

import java.awt.image.BufferedImage;

/**
 * TSPL 单色位图打包工具。
 * <p>
 * 约定：宽度以字节计；bit0=黑（打印）/ bit1=白；每行高位在前（MSB first）。
 */
public final class MonoBitmap {

    private MonoBitmap() {
    }

    /**
     * 将图像打包为 TSPL BITMAP 原始单色字节（非十六进制）。
     * 像素 RGB 按阈值判定：低于 {@code 0x800000} 视为黑（打印）。
     */
    public static byte[] pack(BufferedImage image) {
        if (image == null) {
            return new byte[0];
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int widthBytes = widthBytes(width);
        byte[] data = new byte[widthBytes * height];
        for (int y = 0; y < height; y++) {
            int row = y * widthBytes;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                boolean black = rgb < 0x800000;
                if (!black) {
                    data[row + (x >> 3)] |= (byte) (0x80 >> (x & 7));
                }
            }
        }
        return data;
    }

    /**
     * 按灰度阈值打包。{@code threshold} 范围 0–255，亮度低于阈值视为黑。
     */
    public static byte[] pack(BufferedImage image, int threshold) {
        if (image == null) {
            return new byte[0];
        }
        int t = Math.max(0, Math.min(255, threshold));
        int width = image.getWidth();
        int height = image.getHeight();
        int widthBytes = widthBytes(width);
        byte[] data = new byte[widthBytes * height];
        for (int y = 0; y < height; y++) {
            int row = y * widthBytes;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r * 30 + g * 59 + b * 11) / 100;
                boolean black = gray < t;
                if (!black) {
                    data[row + (x >> 3)] |= (byte) (0x80 >> (x & 7));
                }
            }
        }
        return data;
    }

    public static int widthBytes(int widthDots) {
        return (widthDots + 7) / 8;
    }

    /**
     * 点阵宽度（点）= {@code widthBytes * 8}。末尾不足一字节的填充位同样计入。
     */
    public static int widthDots(int widthBytes) {
        return widthBytes < 0 ? 0 : widthBytes * 8;
    }

    /**
     * 读取像素是否为黑（打印）。越界或数据不足时返回 false（白）。
     * <p>
     * 与 {@link #pack} 约定一致：bit=0 黑，bit=1 白；行内 MSB 在前。
     */
    public static boolean isBlack(byte[] data, int widthBytes, int x, int y) {
        if (data == null || widthBytes <= 0 || x < 0 || y < 0) {
            return false;
        }
        int index = y * widthBytes + (x >> 3);
        if (index >= data.length) {
            return false;
        }
        int mask = 0x80 >> (x & 7);
        return (data[index] & mask) == 0;
    }
}
