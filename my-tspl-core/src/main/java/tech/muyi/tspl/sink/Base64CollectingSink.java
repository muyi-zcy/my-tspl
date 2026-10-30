package tech.muyi.tspl.sink;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * 将每次写入收集为一条 Base64 字符串——供前端 / 本地 PrintServer 下发。
 */
public final class Base64CollectingSink implements TsplSink {

    private final List<String> commands = new ArrayList<>();
    private boolean closed;

    @Override
    public void write(byte[] payload) {
        ensureOpen();
        byte[] data = payload == null ? new byte[0] : payload;
        commands.add(Base64.getEncoder().encodeToString(data));
    }

    public List<String> peekBase64() {
        return Collections.unmodifiableList(new ArrayList<>(commands));
    }

    /**
     * 快照并清空已收集的指令。
     */
    public List<String> drainBase64() {
        List<String> result = new ArrayList<>(commands);
        commands.clear();
        return result;
    }

    public void clear() {
        commands.clear();
    }

    public int size() {
        return commands.size();
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Base64CollectingSink is closed");
        }
    }
}
