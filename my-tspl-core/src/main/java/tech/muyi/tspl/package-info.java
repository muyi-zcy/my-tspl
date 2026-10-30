/**
 * my-tspl 核心入口：{@link tech.muyi.tspl.TsplDocument}、{@link tech.muyi.tspl.TsplDpi}、
 * {@link tech.muyi.tspl.TsplCharsets}。
 * <p>
 * 典型流水线：{@code TsplLabel} / {@code TsplTemplate} → {@code TsplDocument}
 * → {@code TsplEncoder} → {@code TsplClient} / {@code TsplSink}。
 */
package tech.muyi.tspl;
