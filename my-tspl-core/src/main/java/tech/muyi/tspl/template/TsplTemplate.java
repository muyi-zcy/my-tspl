package tech.muyi.tspl.template;

import tech.muyi.tspl.TsplCharsets;
import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.TsplDpi;
import tech.muyi.tspl.TsplException;
import tech.muyi.tspl.cmd.RawCmd;
import tech.muyi.tspl.cmd.TsplCmd;
import tech.muyi.tspl.encode.TsplEncoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本模板：支持 {@code ${placeholder}} 绑定，以及可选的按行钩子插入指令。
 */
public final class TsplTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");

    private final TsplTemplateSource source;
    private final Charset charset;

    private TsplTemplate(TsplTemplateSource source, Charset charset) {
        this.source = Objects.requireNonNull(source, "source");
        this.charset = charset == null ? TsplCharsets.GB2312 : charset;
    }

    public static TsplTemplate of(TsplTemplateSource source) {
        return new TsplTemplate(source, TsplCharsets.GB2312);
    }

    public TsplTemplate charset(Charset charset) {
        return new TsplTemplate(source, charset);
    }

    public Bound load(String location) {
        try {
            String content = source.load(location);
            return new Bound(content, charset);
        } catch (TsplTemplateNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new TsplException("failed to load template: " + location, e);
        }
    }

    /**
     * 基于已加载的模板正文绑定（跳过 {@link TsplTemplateSource}）。
     */
    public static Bound fromContent(String content) {
        return new Bound(content == null ? "" : content, TsplCharsets.GB2312);
    }

    public static final class Bound {
        private final String content;
        private final Charset charset;
        private TsplDpi dpi = TsplDpi.dpi203();
        private final Map<String, String> values = new HashMap<>();
        private final List<LineHook> hooks = new ArrayList<>();

        private Bound(String content, Charset charset) {
            this.content = content;
            this.charset = charset;
        }

        public Bound charset(Charset charset) {
            Bound next = new Bound(content, charset);
            next.dpi = this.dpi;
            next.values.putAll(this.values);
            next.hooks.addAll(this.hooks);
            return next;
        }

        public Bound dpi(int dpi) {
            this.dpi = TsplDpi.of(dpi);
            return this;
        }

        public Bound dpi(TsplDpi dpi) {
            this.dpi = dpi == null ? TsplDpi.dpi203() : dpi;
            return this;
        }

        public Bound bind(String key, String value) {
            values.put(key, value == null ? "" : value);
            return this;
        }

        public Bound bind(Map<String, String> map) {
            if (map != null) {
                for (Map.Entry<String, String> e : map.entrySet()) {
                    bind(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        public Bound insertBefore(String linePrefix, TsplCmd... cmds) {
            return insertBefore(line -> line.startsWith(linePrefix), cmds);
        }

        public Bound insertBefore(Predicate<String> lineMatcher, TsplCmd... cmds) {
            hooks.add(new LineHook(true, lineMatcher, copyCmds(cmds)));
            return this;
        }

        public Bound insertAfter(String linePrefix, TsplCmd... cmds) {
            return insertAfter(line -> line.startsWith(linePrefix), cmds);
        }

        public Bound insertAfter(Predicate<String> lineMatcher, TsplCmd... cmds) {
            hooks.add(new LineHook(false, lineMatcher, copyCmds(cmds)));
            return this;
        }

        public TsplDocument toDocument() {
            String filled = normalizeTspl(fill(content, values));
            String[] lines = filled.split("\r\n", -1);
            TsplDocument.Builder builder = TsplDocument.builder().charset(charset).dpi(dpi);
            boolean[] hookUsed = new boolean[hooks.size()];

            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < hooks.size(); i++) {
                    LineHook hook = hooks.get(i);
                    if (!hook.before || hookUsed[i] || !hook.matcher.test(line)) {
                        continue;
                    }
                    builder.addAll(hook.cmds);
                    hookUsed[i] = true;
                }
                builder.add(RawCmd.line(line));
                for (int i = 0; i < hooks.size(); i++) {
                    LineHook hook = hooks.get(i);
                    if (hook.before || hookUsed[i] || !hook.matcher.test(line)) {
                        continue;
                    }
                    builder.addAll(hook.cmds);
                    hookUsed[i] = true;
                }
            }

            for (int i = 0; i < hooks.size(); i++) {
                if (!hookUsed[i]) {
                    builder.addAll(hooks.get(i).cmds);
                }
            }
            return builder.build();
        }

        public byte[] toBytes() {
            return TsplEncoder.encode(toDocument());
        }

        public String toBase64() {
            return TsplEncoder.encodeBase64(toDocument());
        }

        public String filledText() {
            return normalizeTspl(fill(content, values));
        }
    }

    private static List<TsplCmd> copyCmds(TsplCmd[] cmds) {
        if (cmds == null || cmds.length == 0) {
            return Collections.emptyList();
        }
        List<TsplCmd> list = new ArrayList<>(cmds.length);
        for (TsplCmd cmd : cmds) {
            if (cmd != null) {
                list.add(cmd);
            }
        }
        return list;
    }

    private static String fill(String template, Map<String, String> data) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = data.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String normalizeTspl(String command) {
        String normalized = command.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
        if (!normalized.isEmpty() && !normalized.endsWith("\r\n")) {
            normalized = normalized + "\r\n";
        }
        return normalized;
    }

    private static final class LineHook {
        private final boolean before;
        private final Predicate<String> matcher;
        private final List<TsplCmd> cmds;

        private LineHook(boolean before, Predicate<String> matcher, List<TsplCmd> cmds) {
            this.before = before;
            this.matcher = Objects.requireNonNull(matcher, "matcher");
            this.cmds = cmds;
        }
    }
}
