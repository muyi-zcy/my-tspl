package tech.muyi.tspl;

import tech.muyi.tspl.cmd.BitmapCmd;
import tech.muyi.tspl.cmd.DownloadCmd;
import tech.muyi.tspl.cmd.RawCmd;
import tech.muyi.tspl.cmd.TextLineCmd;
import tech.muyi.tspl.cmd.TsplCmd;
import tech.muyi.tspl.encode.TsplEncoder;
import tech.muyi.tspl.io.TsplOutput;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 有序的 TSPL 指令序列。调用 {@link #build()} 后不可变。
 * <p>
 * {@link #dpi()} 记录排版所用打印机分辨率，供预览与下游换算毫米→点；
 * 指令本身不含 DPI（TSPL 坐标始终是点）。
 */
public final class TsplDocument {

    private final List<TsplCmd> commands;
    private final Charset charset;
    private final TsplDpi dpi;

    private TsplDocument(List<TsplCmd> commands, Charset charset, TsplDpi dpi) {
        this.commands = Collections.unmodifiableList(new ArrayList<>(commands));
        this.charset = charset == null ? TsplCharsets.GB2312 : charset;
        this.dpi = dpi == null ? TsplDpi.dpi203() : dpi;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<TsplCmd> commands() {
        return commands;
    }

    public Charset charset() {
        return charset;
    }

    /**
     * 构建时声明的打印机 DPI（默认 203）。
     */
    public TsplDpi dpi() {
        return dpi;
    }

    public byte[] toBytes() {
        return TsplEncoder.encode(this);
    }

    public String toBase64() {
        return TsplEncoder.encodeBase64(this);
    }

    /**
     * 输出可读命令行列表（二进制指令为摘要，不含原始载荷字节）。
     */
    public List<String> toCommandLines() {
        List<String> lines = new ArrayList<>(commands.size());
        for (TsplCmd cmd : commands) {
            String line = describe(cmd);
            if (line != null && !line.isEmpty()) {
                lines.add(line);
            }
        }
        return Collections.unmodifiableList(lines);
    }

    /**
     * 输出可读 TSPL 命令文本，行以 {@code \r\n} 分隔。
     * 二进制指令（BITMAP / DOWNLOAD / raw bytes）以摘要形式表示，便于调试与日志。
     */
    public String toCommands() {
        List<String> lines = toCommandLines();
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(lines.size() * 32);
        for (String line : lines) {
            sb.append(line).append("\r\n");
        }
        return sb.toString();
    }

    public void writeTo(TsplOutput out) {
        for (TsplCmd cmd : commands) {
            cmd.writeTo(out);
        }
    }

    @Override
    public String toString() {
        return toCommands();
    }

    private static String describe(TsplCmd cmd) {
        if (cmd == null) {
            return "";
        }
        if (cmd instanceof TextLineCmd) {
            return ((TextLineCmd) cmd).line();
        }
        if (cmd instanceof BitmapCmd) {
            return ((BitmapCmd) cmd).header();
        }
        if (cmd instanceof DownloadCmd) {
            return ((DownloadCmd) cmd).header();
        }
        if (cmd instanceof RawCmd) {
            return cmd.toString();
        }
        String s = cmd.toString();
        if (s == null || s.isEmpty() || s.contains(cmd.getClass().getName())) {
            return "<cmd>";
        }
        return s;
    }

    public static final class Builder {
        private final List<TsplCmd> commands = new ArrayList<>();
        private Charset charset = TsplCharsets.GB2312;
        private TsplDpi dpi = TsplDpi.dpi203();

        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder dpi(int dpi) {
            this.dpi = TsplDpi.of(dpi);
            return this;
        }

        public Builder dpi(TsplDpi dpi) {
            this.dpi = dpi == null ? TsplDpi.dpi203() : dpi;
            return this;
        }

        public Builder add(TsplCmd cmd) {
            if (cmd != null) {
                commands.add(cmd);
            }
            return this;
        }

        public Builder addAll(Iterable<? extends TsplCmd> cmds) {
            if (cmds != null) {
                for (TsplCmd cmd : cmds) {
                    add(cmd);
                }
            }
            return this;
        }

        public TsplDocument build() {
            return new TsplDocument(commands, charset, dpi);
        }
    }
}
