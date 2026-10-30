package tech.muyi.tspl.sink;

import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.encode.TsplEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * 两种主要下发模式的门面：直连打印机发送，以及面向前端的 Base64。
 */
public final class TsplClient implements AutoCloseable {

    private final TsplSink sink;
    private final Base64CollectingSink base64Sink;

    private TsplClient(TsplSink sink, Base64CollectingSink base64Sink) {
        this.sink = sink;
        this.base64Sink = base64Sink;
    }

    /**
     * 仅收集 Base64 指令（由前端自行连接打印机）。
     */
    public static TsplClient base64() {
        Base64CollectingSink b64 = new Base64CollectingSink();
        return new TsplClient(b64, b64);
    }

    /**
     * 直接发送到网络打印机。
     */
    public static TsplClient printer(String host, int port) {
        return printer(host, port, 2000);
    }

    public static TsplClient printer(String host, int port, int timeoutMs) {
        return new TsplClient(SocketPrinterSink.connect(host, port, timeoutMs), null);
    }

    /**
     * 双模式：始终收集 Base64；若提供 host/port 则同时发往打印机。
     *
     * @param host 打印机主机；{@code null} 或空白时仅收集 Base64
     * @param port 打印机端口（host 为空时忽略）
     */
    public static TsplClient dual(String host, Integer port) {
        return dual(host, port, 2000);
    }

    public static TsplClient dual(String host, Integer port, int timeoutMs) {
        Base64CollectingSink b64 = new Base64CollectingSink();
        if (host == null || host.trim().isEmpty() || port == null) {
            return new TsplClient(b64, b64);
        }
        SocketPrinterSink socket = SocketPrinterSink.connect(host, port, timeoutMs);
        return new TsplClient(new CompositeSink(b64, socket), b64);
    }

    public TsplClient send(TsplDocument document) {
        sink.write(document);
        return this;
    }

    public TsplClient send(byte[] payload) {
        sink.write(payload);
        return this;
    }

    public TsplClient sendAll(Iterable<TsplDocument> documents) {
        sink.writeAll(documents);
        return this;
    }

    public TsplClient sendAllBytes(Iterable<byte[]> payloads) {
        sink.writeAllBytes(payloads);
        return this;
    }

    /**
     * 取出并清空已收集的 Base64 指令。纯打印机客户端时返回空列表。
     */
    public List<String> drainBase64() {
        if (base64Sink == null) {
            return new ArrayList<>();
        }
        return base64Sink.drainBase64();
    }

    public List<String> peekBase64() {
        if (base64Sink == null) {
            return new ArrayList<>();
        }
        return base64Sink.peekBase64();
    }

    /**
     * 将文档编码为 Base64，不打开 sink（无状态辅助方法）。
     */
    public static List<String> encodeBase64(Iterable<TsplDocument> documents) {
        return TsplEncoder.encodeBase64List(documents);
    }

    @Override
    public void close() {
        sink.close();
    }

    /**
     * 便于 try-with-resources 使用的包装方法。
     */
    public static List<String> withBase64(ClientAction action) {
        TsplClient client = base64();
        try {
            action.run(client);
            return client.drainBase64();
        } finally {
            client.close();
        }
    }

    public static List<String> withDual(String host, Integer port, ClientAction action) {
        TsplClient client = dual(host, port);
        try {
            action.run(client);
            return client.drainBase64();
        } finally {
            client.close();
        }
    }

    @FunctionalInterface
    public interface ClientAction {
        void run(TsplClient client);
    }
}
