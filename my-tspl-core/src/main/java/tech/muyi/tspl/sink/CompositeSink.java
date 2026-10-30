package tech.muyi.tspl.sink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 扇出：将同一载荷写入多个 sink（例如 Base64 + Socket）。
 */
public final class CompositeSink implements TsplSink {

    private final List<TsplSink> sinks;

    public CompositeSink(TsplSink... sinks) {
        this.sinks = new ArrayList<>();
        if (sinks != null) {
            this.sinks.addAll(Arrays.asList(sinks));
        }
    }

    public CompositeSink(List<TsplSink> sinks) {
        this.sinks = new ArrayList<>();
        if (sinks != null) {
            this.sinks.addAll(sinks);
        }
    }

    @Override
    public void write(byte[] payload) {
        for (TsplSink sink : sinks) {
            if (sink != null) {
                sink.write(payload);
            }
        }
    }

    @Override
    public void close() {
        RuntimeException first = null;
        for (TsplSink sink : sinks) {
            if (sink == null) {
                continue;
            }
            try {
                sink.close();
            } catch (RuntimeException e) {
                if (first == null) {
                    first = e;
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }
}
