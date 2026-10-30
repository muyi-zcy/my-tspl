package tech.muyi.tspl.cmd;

import tech.muyi.tspl.io.TsplOutput;

/**
 * 下载文件指令：DOWNLOAD F,"filename",length,&lt;文件字节&gt;
 */
public final class DownloadCmd implements TsplCmd {

    private final String filename;
    private final byte[] fileData;

    public DownloadCmd(String filename, byte[] fileData) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename must not be empty");
        }
        this.filename = filename;
        this.fileData = fileData == null ? new byte[0] : fileData;
    }

    public String filename() {
        return filename;
    }

    public byte[] fileData() {
        return fileData;
    }

    /**
     * 可读摘要（不含文件原始字节）。
     */
    public String header() {
        return "DOWNLOAD F,\"" + filename + "\"," + fileData.length + ",<" + fileData.length + " bytes>";
    }

    @Override
    public void writeTo(TsplOutput out) {
        out.writeAscii("DOWNLOAD F,\"" + filename + "\"," + fileData.length + ",");
        out.write(fileData);
        out.writeCrLf();
    }

    @Override
    public String toString() {
        return header();
    }
}
