package tech.muyi.tspl.template;

import java.io.IOException;

/**
 * 可插拔的模板加载器。{@code location} 由具体实现解释为逻辑名称
 * （classpath 路径、文件名、数据库编码、OSS key 等）。
 */
@FunctionalInterface
public interface TsplTemplateSource {

    /**
     * 加载模板文本（通常为 UTF-8）。
     *
     * @throws TsplTemplateNotFoundException 无法解析 location 时抛出
     * @throws IOException                   资源已找到但 I/O 失败时抛出
     */
    String load(String location) throws IOException;
}
