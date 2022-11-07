package juuxel.kelp;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.io.IOException;
import java.util.Map;

public record TranslationFile(Map<String, String> translations) {
    private static final String INDENT = " ".repeat(2);

    public void toJson(FormatFile format, Appendable app) throws IOException {
        app.append("{");

        TranslationKeyComparator comparator = format.getTranslationKeyComparator();
        TranslationGroup group = null;

        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (group != null) {
                app.append(",");

                TranslationGroup current = comparator.getGroup(entry.getKey());

                if (current != group) {
                    group = current;
                    app.append('\n');
                }
            } else {
                group = comparator.getGroup(entry.getKey());
            }

            app.append('\n').append(INDENT).append('"')
                .append(escape(entry.getKey()))
                .append("\": \"")
                .append(escape(entry.getValue()))
                .append("\"");
        }

        app.append("\n}\n");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\"", "\\\"");
    }

    public static final class Adapter implements JsonAdapter<TranslationFile, Map<String, String>> {
        @Override
        @FromJson
        public TranslationFile fromJson(Map<String, String> translations) {
            return new TranslationFile(translations);
        }

        @Override
        @ToJson
        public Map<String, String> toJson(TranslationFile value) {
            return value.translations();
        }
    }
}
