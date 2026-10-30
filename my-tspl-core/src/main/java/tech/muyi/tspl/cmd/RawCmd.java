package tech.muyi.tspl.cmd;

import tech.muyi.tspl.io.TsplOutput;

/**
 * 逃生舱：按原样追加原始文本行或原始字节。
 */
public final class RawCmd implements TsplCmd {

    private final String line;
    private final byte[] bytes;
    private final boolean appendCrLf;

    private RawCmd(String line, byte[] bytes, boolean appendCrLf) {
        this.line = line;
        this.bytes = bytes;
        this.appendCrLf = appendCrLf;
    }

    public static RawCmd line(String commandLine) {
        return new RawCmd(commandLine, null, true);
    }

    public static RawCmd bytes(byte[] payload) {
        return new RawCmd(null, payload == null ? new byte[0] : payload, false);
    }

    public static RawCmd bytesWithCrLf(byte[] payload) {
        return new RawCmd(null, payload == null ? new byte[0] : payload, true);
    }

    @Override
    public void writeTo(TsplOutput out) {
        if (bytes != null) {
            out.write(bytes);
            if (appendCrLf) {
                out.writeCrLf();
            }
            return;
        }
        if (appendCrLf) {
            out.writeLine(line);
        } else {
            out.writeText(line);
        }
    }

    @Override
    public String toString() {
        if (bytes != null) {
            return "<raw " + bytes.length + " bytes>";
        }
        return line == null ? "" : line;
    }
}
