package tech.muyi.tspl.preview;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 测试预览 PNG 默认输出目录：{@code target/preview/}。
 */
public final class PreviewPaths {

    private PreviewPaths() {
    }

    public static Path dir() {
        return Paths.get("target", "preview");
    }

    public static Path file(String name) {
        return dir().resolve(name);
    }
}
