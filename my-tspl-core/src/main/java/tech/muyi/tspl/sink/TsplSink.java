package tech.muyi.tspl.sink;

import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.encode.TsplEncoder;

/**
 * 已编码 TSPL 载荷的下发通道。
 */
public interface TsplSink extends AutoCloseable {

    void write(byte[] payload);

    default void write(TsplDocument document) {
        write(TsplEncoder.encode(document));
    }

    default void writeAll(Iterable<TsplDocument> documents) {
        if (documents == null) {
            return;
        }
        for (TsplDocument document : documents) {
            write(document);
        }
    }

    default void writeAllBytes(Iterable<byte[]> payloads) {
        if (payloads == null) {
            return;
        }
        for (byte[] payload : payloads) {
            write(payload);
        }
    }

    @Override
    void close();
}
