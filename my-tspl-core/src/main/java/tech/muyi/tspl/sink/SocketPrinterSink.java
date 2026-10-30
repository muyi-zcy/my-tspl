package tech.muyi.tspl.sink;

import tech.muyi.tspl.TsplException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 将载荷直接发送到网络 TSPL 打印机。
 */
public final class SocketPrinterSink implements TsplSink {

    private final Socket socket;
    private final OutputStream out;
    private boolean closed;

    private SocketPrinterSink(Socket socket, OutputStream out) {
        this.socket = socket;
        this.out = out;
    }

    public static SocketPrinterSink connect(String host, int port) {
        return connect(host, port, 2000);
    }

    public static SocketPrinterSink connect(String host, int port, int timeoutMs) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return new SocketPrinterSink(socket, socket.getOutputStream());
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 忽略关闭异常
            }
            throw new TsplException("failed to connect printer " + host + ":" + port, e);
        }
    }

    @Override
    public void write(byte[] payload) {
        ensureOpen();
        if (payload == null || payload.length == 0) {
            return;
        }
        try {
            out.write(payload);
            out.flush();
        } catch (IOException e) {
            throw new TsplException("failed to write to printer", e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            out.close();
        } catch (IOException ignored) {
            // 忽略关闭异常
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // 忽略关闭异常
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("SocketPrinterSink is closed");
        }
    }
}
