package tech.muyi.tspl.cmd;

import tech.muyi.tspl.io.TsplOutput;

/**
 * 简单文本指令，以一行 CRLF 结尾写出。
 */
public final class TextLineCmd implements TsplCmd {

    private final String line;

    public TextLineCmd(String line) {
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException("command line must not be empty");
        }
        this.line = line;
    }

    public String line() {
        return line;
    }

    @Override
    public void writeTo(TsplOutput out) {
        out.writeLine(line);
    }

    @Override
    public String toString() {
        return line;
    }
}
