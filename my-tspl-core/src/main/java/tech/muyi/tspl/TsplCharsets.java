package tech.muyi.tspl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 与 TSC 打印机约定一致的 TSPL 字符集常量。
 */
public final class TsplCharsets {

    /**
     * 中文文本 / 指令头编码——对应常见 TSC GB2312 固件。
     */
    public static final Charset GB2312 = Charset.forName("GB2312");

    /**
     * 仅 ASCII 的指令头（BITMAP、DOWNLOAD 长度前缀等）。
     */
    public static final Charset ASCII = StandardCharsets.US_ASCII;

    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    private TsplCharsets() {
    }
}
