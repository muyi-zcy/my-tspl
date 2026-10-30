package tech.muyi.tspl;

import org.junit.Test;
import tech.muyi.tspl.cmd.TsplCmds;
import tech.muyi.tspl.render.TextBitmapRenderer;
import tech.muyi.tspl.render.TextStyle;
import tech.muyi.tspl.template.TsplTemplate;
import tech.muyi.tspl.template.TsplTemplateNotFoundException;
import tech.muyi.tspl.template.TsplTemplateSource;
import tech.muyi.tspl.template.TsplTemplateSources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TsplTemplateTest {

    @Test
    public void classpathSourceLoadsAndBinds() {
        TsplDocument doc = TsplTemplate.of(TsplTemplateSources.classpath("tsc"))
                .load("barcode_label.tspl")
                .bind("traycode", "B3-1234")
                .toDocument();

        String text = new String(doc.toBytes(), TsplCharsets.GB2312);
        assertTrue(text.contains("QRCODE 680,110,L,20,A,0,M2,\"B3-1234\""));
        assertTrue(text.contains("PRINT 1,1"));
    }

    @Test
    public void customSourceIsUsed() {
        TsplTemplateSource custom = location -> {
            assertEquals("my_label", location);
            return "CLS\nTEXT 0,0,\"0\",0,1,1,\"${name}\"\nPRINT 1\n";
        };

        String text = TsplTemplate.of(custom)
                .load("my_label")
                .bind(Collections.singletonMap("name", "Hello"))
                .filledText();

        assertTrue(text.contains("\"Hello\""));
    }

    @Test
    public void insertBeforeQrcode() {
        byte[] bytes = TsplTemplate.of(TsplTemplateSources.classpath("tsc"))
                .load("barcode_label.tspl")
                .bind("traycode", "X1")
                .insertBefore("QRCODE",
                        TextBitmapRenderer.command(50, 30, "铁托盘", TextStyle.bold(24)),
                        TsplCmds.raw("REM MARKER"))
                .toBytes();

        String probe = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        int bitmapAt = probe.indexOf("BITMAP ");
        int qrAt = probe.indexOf("QRCODE ");
        int remAt = probe.indexOf("REM MARKER");
        assertTrue(bitmapAt >= 0);
        assertTrue(qrAt > bitmapAt);
        assertTrue(remAt > bitmapAt && remAt < qrAt);
    }

    @Test
    public void firstOfFallsBack() {
        Map<String, String> map = new HashMap<>();
        map.put("a.tspl", "CLS\r\nPRINT 1\r\n");
        TsplTemplateSource source = TsplTemplateSources.firstOf(
                location -> {
                    throw new TsplTemplateNotFoundException(location);
                },
                TsplTemplateSources.map(map)
        );
        String text = TsplTemplate.of(source).load("a.tspl").filledText();
        assertTrue(text.startsWith("CLS"));
    }

    @Test(expected = TsplTemplateNotFoundException.class)
    public void missingClasspathThrows() {
        TsplTemplate.of(TsplTemplateSources.classpath("tsc")).load("missing.tspl");
    }
}
