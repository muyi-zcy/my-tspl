package tech.muyi.tspl.cmd;

import tech.muyi.tspl.io.TsplOutput;

/**
 * 一条 TSPL 指令（文本和/或二进制载荷）。
 */
@FunctionalInterface
public interface TsplCmd {

    void writeTo(TsplOutput out);
}
