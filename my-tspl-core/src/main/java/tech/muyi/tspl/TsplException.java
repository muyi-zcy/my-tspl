package tech.muyi.tspl;

/**
 * TSPL 构建 / 编码 / 下发过程中的非检查异常。
 */
public class TsplException extends RuntimeException {

    public TsplException(String message) {
        super(message);
    }

    public TsplException(String message, Throwable cause) {
        super(message, cause);
    }
}
