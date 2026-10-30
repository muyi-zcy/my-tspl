package tech.muyi.tspl.encode;

import tech.muyi.tspl.TsplDocument;
import tech.muyi.tspl.io.TsplOutput;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * 将 {@link TsplDocument} 序列化为线路字节 / Base64。
 */
public final class TsplEncoder {

    private TsplEncoder() {
    }

    public static byte[] encode(TsplDocument document) {
        if (document == null) {
            return new byte[0];
        }
        TsplOutput out = new TsplOutput(document.charset());
        document.writeTo(out);
        return out.toByteArray();
    }

    public static String encodeBase64(TsplDocument document) {
        return Base64.getEncoder().encodeToString(encode(document));
    }

    public static List<String> encodeBase64List(Iterable<TsplDocument> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (TsplDocument doc : documents) {
            list.add(encodeBase64(doc));
        }
        return list;
    }

    public static List<String> encodeBase64List(byte[]... payloads) {
        if (payloads == null || payloads.length == 0) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(payloads.length);
        Base64.Encoder encoder = Base64.getEncoder();
        for (byte[] payload : payloads) {
            list.add(encoder.encodeToString(payload == null ? new byte[0] : payload));
        }
        return list;
    }
}
