package tech.muyi.tspl.cmd;

/**
 * 常用 TSPL 文本 / 二进制指令的工厂方法。
 */
public final class TsplCmds {

    private TsplCmds() {
    }

    public static TsplCmd cls() {
        return new TextLineCmd("CLS");
    }

    public static TsplCmd sizeMm(double widthMm, double heightMm) {
        return new TextLineCmd("SIZE " + trimNum(widthMm) + " mm, " + trimNum(heightMm) + " mm");
    }

    public static TsplCmd sizeInch(double widthInch, double heightInch) {
        return new TextLineCmd("SIZE " + trimNum(widthInch) + ", " + trimNum(heightInch));
    }

    public static TsplCmd gapMm(double gapMm, double offsetMm) {
        return new TextLineCmd("GAP " + trimNum(gapMm) + " mm, " + trimNum(offsetMm) + " mm");
    }

    public static TsplCmd direction(int direction, int mirror) {
        return new TextLineCmd("DIRECTION " + direction + "," + mirror);
    }

    public static TsplCmd reference(int x, int y) {
        return new TextLineCmd("REFERENCE " + x + "," + y);
    }

    public static TsplCmd offsetMm(double offsetMm) {
        return new TextLineCmd("OFFSET " + trimNum(offsetMm) + " mm");
    }

    public static TsplCmd setPeel(boolean on) {
        return set(nameOnOff("PEEL", on));
    }

    public static TsplCmd setCutter(boolean on) {
        return set(nameOnOff("CUTTER", on));
    }

    public static TsplCmd setPartialCutter(boolean on) {
        return set(nameOnOff("PARTIAL_CUTTER", on));
    }

    public static TsplCmd setTear(boolean on) {
        return set(nameOnOff("TEAR", on));
    }

    public static TsplCmd set(String body) {
        return new TextLineCmd("SET " + body);
    }

    public static TsplCmd setFlag(String name, boolean on) {
        return set(nameOnOff(name, on));
    }

    public static TsplCmd speed(int ips) {
        return new TextLineCmd("SPEED " + ips);
    }

    public static TsplCmd density(int density) {
        return new TextLineCmd("DENSITY " + density);
    }

    /**
     * 二维码指令：QRCODE x,y,ECC,cellWidth,mode,rotation[,model,]["content"]
     */
    public static TsplCmd qrcode(int x, int y, String ecc, int cellWidth, String mode,
                                 int rotation, String content) {
        return qrcode(x, y, ecc, cellWidth, mode, rotation, null, content);
    }

    public static TsplCmd qrcode(int x, int y, String ecc, int cellWidth, String mode,
                                 int rotation, String model, String content) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("QRCODE ").append(x).append(',').append(y).append(',')
                .append(ecc).append(',').append(cellWidth).append(',')
                .append(mode).append(',').append(rotation);
        if (model != null && !model.isEmpty()) {
            sb.append(',').append(model);
        }
        sb.append(",\"").append(escapeQuotes(content)).append('"');
        return new TextLineCmd(sb.toString());
    }

    /**
     * 条码指令：BARCODE x,y,"codeType",height,humanReadable,rotation,narrow,wide,"content"
     */
    public static TsplCmd barcode(int x, int y, String codeType, int height,
                                  int humanReadable, int rotation,
                                  int narrow, int wide, String content) {
        String line = "BARCODE " + x + "," + y + ",\"" + codeType + "\","
                + height + "," + humanReadable + "," + rotation + ","
                + narrow + "," + wide + ",\"" + escapeQuotes(content) + '"';
        return new TextLineCmd(line);
    }

    public static TsplCmd text(int x, int y, String font, int rotation,
                               int xMul, int yMul, String content) {
        String line = "TEXT " + x + "," + y + ",\"" + font + "\","
                + rotation + "," + xMul + "," + yMul + ",\""
                + escapeQuotes(content) + '"';
        return new TextLineCmd(line);
    }

    /**
     * 直线：LINE x0,y0,x1,y1,thickness
     */
    public static TsplCmd line(int x0, int y0, int x1, int y1, int thickness) {
        return new TextLineCmd("LINE " + x0 + "," + y0 + "," + x1 + "," + y1 + ","
                + clampThickness(thickness));
    }

    /**
     * 方框：BOX x0,y0,x1,y1,thickness
     */
    public static TsplCmd box(int x0, int y0, int x1, int y1, int thickness) {
        return new TextLineCmd("BOX " + x0 + "," + y0 + "," + x1 + "," + y1 + ","
                + clampThickness(thickness));
    }

    /**
     * 水平线便利方法（内部生成 LINE）。
     */
    public static TsplCmd hline(int x, int y, int length, int thickness) {
        return line(x, y, x + length, y, thickness);
    }

    /**
     * 垂直线便利方法（内部生成 LINE）。
     */
    public static TsplCmd vline(int x, int y, int length, int thickness) {
        return line(x, y, x, y + length, thickness);
    }

    public static TsplCmd print(int sets, int copies) {
        return new TextLineCmd("PRINT " + sets + "," + copies);
    }

    public static TsplCmd print(int sets) {
        return print(sets, 1);
    }

    /**
     * 内联 BITMAP（原始单色字节，非十六进制）。
     */
    public static TsplCmd bitmap(int x, int y, int widthBytes, int height, int mode, byte[] data) {
        return new BitmapCmd(x, y, widthBytes, height, mode, data);
    }

    /**
     * 下载文件到打印机：DOWNLOAD F,"filename",length,&lt;bytes&gt;
     */
    public static TsplCmd downloadFile(String filename, byte[] fileData) {
        return new DownloadCmd(filename, fileData);
    }

    public static TsplCmd raw(String line) {
        return RawCmd.line(line);
    }

    public static TsplCmd rawBytes(byte[] payload) {
        return RawCmd.bytes(payload);
    }

    private static String nameOnOff(String name, boolean on) {
        return name + " " + (on ? "ON" : "OFF");
    }

    private static String escapeQuotes(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\"", "\\\"");
    }

    private static int clampThickness(int thickness) {
        return thickness < 1 ? 1 : thickness;
    }

    private static String trimNum(double v) {
        if (v == (long) v) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }
}
