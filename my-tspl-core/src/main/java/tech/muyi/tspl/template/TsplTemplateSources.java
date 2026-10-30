package tech.muyi.tspl.template;

import tech.muyi.tspl.TsplCharsets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置 {@link TsplTemplateSource} 工厂。
 */
public final class TsplTemplateSources {

    private TsplTemplateSources() {
    }

    public static TsplTemplateSource classpath() {
        return classpath(Thread.currentThread().getContextClassLoader(), "");
    }

    public static TsplTemplateSource classpath(String basePackageOrDir) {
        return classpath(Thread.currentThread().getContextClassLoader(), basePackageOrDir);
    }

    public static TsplTemplateSource classpath(ClassLoader classLoader) {
        return classpath(classLoader, "");
    }

    /**
     * 在 classpath 可选基目录下解析 {@code location}。
     * 示例：base 为 {@code tsc}，location 为 {@code barcode_label.tspl} → {@code tsc/barcode_label.tspl}。
     */
    public static TsplTemplateSource classpath(ClassLoader classLoader, String baseDir) {
        ClassLoader cl = classLoader == null ? TsplTemplateSources.class.getClassLoader() : classLoader;
        String base = normalizeBase(baseDir);
        return location -> {
            String path = join(base, location);
            try (InputStream in = cl.getResourceAsStream(path)) {
                if (in == null) {
                    throw new TsplTemplateNotFoundException(path);
                }
                return readUtf8(in);
            }
        };
    }

    public static TsplTemplateSource file(Path baseDir) {
        Objects.requireNonNull(baseDir, "baseDir");
        return location -> {
            Path path = baseDir.resolve(location);
            if (!Files.isRegularFile(path)) {
                throw new TsplTemplateNotFoundException(path.toString());
            }
            return new String(Files.readAllBytes(path), TsplCharsets.UTF_8);
        };
    }

    public static TsplTemplateSource url() {
        return location -> {
            try {
                URL url = new URL(location);
                try (InputStream in = url.openStream()) {
                    return readUtf8(in);
                }
            } catch (TsplTemplateNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new TsplTemplateNotFoundException(location, e);
            }
        };
    }

    public static TsplTemplateSource string(String content) {
        String fixed = content == null ? "" : content;
        return location -> fixed;
    }

    public static TsplTemplateSource map(Map<String, String> templates) {
        Objects.requireNonNull(templates, "templates");
        return location -> {
            String content = templates.get(location);
            if (content == null) {
                throw new TsplTemplateNotFoundException(location);
            }
            return content;
        };
    }

    public static TsplTemplateSource caching(TsplTemplateSource delegate) {
        Objects.requireNonNull(delegate, "delegate");
        Map<String, String> cache = new ConcurrentHashMap<>();
        return location -> {
            String cached = cache.get(location);
            if (cached != null) {
                return cached;
            }
            String loaded = delegate.load(location);
            cache.put(location, loaded);
            return loaded;
        };
    }

    /**
     * 按顺序尝试多个源，第一个成功加载的胜出。
     */
    public static TsplTemplateSource firstOf(TsplTemplateSource... sources) {
        if (sources == null || sources.length == 0) {
            throw new IllegalArgumentException("at least one source required");
        }
        return location -> {
            TsplTemplateNotFoundException last = null;
            for (TsplTemplateSource source : sources) {
                if (source == null) {
                    continue;
                }
                try {
                    return source.load(location);
                } catch (TsplTemplateNotFoundException e) {
                    last = e;
                }
            }
            if (last != null) {
                throw last;
            }
            throw new TsplTemplateNotFoundException(location);
        };
    }

    private static String normalizeBase(String baseDir) {
        if (baseDir == null || baseDir.isEmpty()) {
            return "";
        }
        String b = baseDir.replace('\\', '/');
        while (b.startsWith("/")) {
            b = b.substring(1);
        }
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b;
    }

    private static String join(String base, String location) {
        String loc = location == null ? "" : location.replace('\\', '/');
        while (loc.startsWith("/")) {
            loc = loc.substring(1);
        }
        if (base.isEmpty()) {
            return loc;
        }
        return base + "/" + loc;
    }

    private static String readUtf8(InputStream in) throws IOException {
        return read(in, TsplCharsets.UTF_8);
    }

    private static String read(InputStream in, Charset charset) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) >= 0) {
            buf.write(chunk, 0, n);
        }
        return new String(buf.toByteArray(), charset);
    }
}
