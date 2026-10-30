package tech.muyi.tspl.cmd;

import tech.muyi.tspl.io.TsplOutput;

/**
 * 位图指令：BITMAP x,y,widthBytes,height,mode,&lt;原始单色字节&gt;
 */
public final class BitmapCmd implements TsplCmd {

    private final int x;
    private final int y;
    private final int widthBytes;
    private final int height;
    private final int mode;
    private final byte[] data;

    public BitmapCmd(int x, int y, int widthBytes, int height, int mode, byte[] data) {
        this.x = x;
        this.y = y;
        this.widthBytes = widthBytes;
        this.height = height;
        this.mode = mode;
        this.data = data == null ? new byte[0] : data;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int widthBytes() {
        return widthBytes;
    }

    public int height() {
        return height;
    }

    public int mode() {
        return mode;
    }

    public byte[] data() {
        return data;
    }

    /**
     * 可读摘要（不含原始位图字节）。
     */
    public String header() {
        return "BITMAP " + x + "," + y + "," + widthBytes + "," + height + "," + mode
                + ",<" + data.length + " bytes>";
    }

    @Override
    public void writeTo(TsplOutput out) {
        out.writeAscii("BITMAP " + x + "," + y + "," + widthBytes + "," + height + "," + mode + ",");
        out.write(data);
        out.writeCrLf();
    }

    @Override
    public String toString() {
        return header();
    }
}
