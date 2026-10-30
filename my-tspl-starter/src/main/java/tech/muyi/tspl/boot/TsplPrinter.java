package tech.muyi.tspl.boot;

import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.TsplException;
import tech.muyi.tspl.encode.TsplEncoder;
import tech.muyi.tspl.sink.TsplClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可复用的打印机长连接：持有一条 TCP，支持同步写与失败自动重连。
 * <p>主机 / 端口由调用方传入，不绑定任何配置源。</p>
 */
public class TsplPrinter implements AutoCloseable {

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 2000;

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final boolean autoReconnect;

    private TsplClient client;
    private boolean closed;
    private volatile long lastSendAtMs;

    public TsplPrinter(String host, int port) {
        this(host, port, DEFAULT_CONNECT_TIMEOUT_MS, true);
    }

    public TsplPrinter(String host, int port, int connectTimeoutMs) {
        this(host, port, connectTimeoutMs, true);
    }

    public TsplPrinter(String host, int port, int connectTimeoutMs, boolean autoReconnect) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("printer host must not be blank");
        }
        this.host = host.trim();
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.autoReconnect = autoReconnect;
        this.client = openClient();
        this.lastSendAtMs = System.currentTimeMillis();
    }

    public synchronized void send(TsplDocument document) {
        ensureOpen();
        execute(() -> client.send(document));
    }

    public synchronized void send(byte[] payload) {
        ensureOpen();
        execute(() -> client.send(payload));
    }

    public synchronized void sendAll(Iterable<TsplDocument> documents) {
        ensureOpen();
        execute(() -> client.sendAll(documents));
    }

    /**
     * 经长连接下发，同时本地编码返回 Base64。
     */
    public synchronized List<String> sendDual(TsplDocument document) {
        send(document);
        return Collections.singletonList(TsplEncoder.encodeBase64(document));
    }

    public synchronized List<String> sendDualAll(Iterable<TsplDocument> documents) {
        List<TsplDocument> snapshot = new ArrayList<>();
        if (documents != null) {
            for (TsplDocument document : documents) {
                snapshot.add(document);
            }
        }
        sendAll(snapshot);
        return TsplEncoder.encodeBase64List(snapshot);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * 最近一次成功发送的时间戳（毫秒，{@link System#currentTimeMillis()}）。
     * 新建连接时初始化为当前时间。
     */
    public long getLastSendAtMs() {
        return lastSendAtMs;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private void execute(Runnable action) {
        try {
            action.run();
            lastSendAtMs = System.currentTimeMillis();
        } catch (RuntimeException first) {
            if (!autoReconnect || closed) {
                throw first;
            }
            reconnectQuietly();
            try {
                action.run();
                lastSendAtMs = System.currentTimeMillis();
            } catch (RuntimeException second) {
                throw new TsplException("printer write failed after reconnect " + host + ":" + port, second);
            }
        }
    }

    private void reconnectQuietly() {
        if (client != null) {
            try {
                client.close();
            } catch (RuntimeException ignored) {
                // 忽略关闭异常，继续重建
            }
            client = null;
        }
        client = openClient();
    }

    private TsplClient openClient() {
        return TsplClient.printer(host, port, connectTimeoutMs);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("TsplPrinter is closed");
        }
        if (client == null) {
            client = openClient();
        }
    }
}
