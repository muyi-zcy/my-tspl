package tech.muyi.tspl.boot;

import tech.muyi.tspl.TsplDocument;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 按 host:port 缓存长连接。地址来源不限（请求参数、DB、配置中心等），由调用方传入。
 */
public class TsplPrinterManager implements AutoCloseable {

    private final int connectTimeoutMs;
    private final boolean autoReconnect;
    private final ConcurrentMap<String, TsplPrinter> printers = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public TsplPrinterManager() {
        this(TsplPrinter.DEFAULT_CONNECT_TIMEOUT_MS, true);
    }

    public TsplPrinterManager(int connectTimeoutMs, boolean autoReconnect) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.autoReconnect = autoReconnect;
    }

    /**
     * 获取或创建指定打印机的长连接（线程安全）。
     */
    public TsplPrinter get(String host, int port) {
        ensureOpen();
        String key = key(host, port);
        return printers.computeIfAbsent(key,
                k -> new TsplPrinter(host, port, connectTimeoutMs, autoReconnect));
    }

    public void send(String host, int port, TsplDocument document) {
        get(host, port).send(document);
    }

    public void send(String host, int port, byte[] payload) {
        get(host, port).send(payload);
    }

    public void sendAll(String host, int port, Iterable<TsplDocument> documents) {
        get(host, port).sendAll(documents);
    }

    public List<String> sendDual(String host, int port, TsplDocument document) {
        return get(host, port).sendDual(document);
    }

    public List<String> sendDualAll(String host, int port, Iterable<TsplDocument> documents) {
        return get(host, port).sendDualAll(documents);
    }

    /**
     * 关闭并移除某一打印机连接。
     */
    public void evict(String host, int port) {
        TsplPrinter printer = printers.remove(key(host, port));
        if (printer != null) {
            printer.close();
        }
    }

    @Override
    public void close() {
        closed = true;
        for (TsplPrinter printer : printers.values()) {
            printer.close();
        }
        printers.clear();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("TsplPrinterManager is closed");
        }
    }

    private static String key(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("printer host must not be blank");
        }
        return host.trim() + ":" + port;
    }
}
