package tech.muyi.tspl.io;

import tech.muyi.tspl.TsplCharsets;
import tech.muyi.tspl.TsplException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 序列化 {@link tech.muyi.tspl.cmd.TsplCmd} 时使用的字节输出缓冲。
 */
public final class TsplOutput {

    private static final byte[] CRLF = {'\r', '\n'};

    private final ByteArrayOutputStream out;
    private final Charset charset;

    public TsplOutput() {
        this(TsplCharsets.GB2312);
    }

    public TsplOutput(Charset charset) {
        this.out = new ByteArrayOutputStream(512);
        this.charset = charset == null ? TsplCharsets.GB2312 : charset;
    }

    public Charset charset() {
        return charset;
    }

    public void write(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new TsplException("write failed", e);
        }
    }

    public void writeAscii(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        write(text.getBytes(TsplCharsets.ASCII));
    }

    public void writeText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        write(text.getBytes(charset));
    }

    /**
     * 写出完整文本指令行（以 CRLF 结尾）。
     */
    public void writeLine(String line) {
        writeText(line == null ? "" : line);
        writeCrLf();
    }

    public void writeCrLf() {
        write(CRLF);
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }

    public int size() {
        return out.size();
    }
}
