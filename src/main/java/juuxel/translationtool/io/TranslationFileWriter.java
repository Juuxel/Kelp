/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.io;

import juuxel.translationtool.schema.Schema;

import java.io.IOException;
import java.util.Map;

public final class TranslationFileWriter {
    public static void write(Appendable appendable, Map<String, String> translations, Schema schema) throws IOException {
        appendable.append("{\n");

        var writtenEntries = schema.entries()
            .stream()
            .filter(entry -> !(entry instanceof Schema.Key(var key)) || translations.containsKey(key))
            .toList();

        for (int i = 0; i < writtenEntries.size(); i++) {
            switch (writtenEntries.get(i)) {
                case Schema.Key(var key):
                    var value = translations.get(key);
                    appendable.append("  ");
                    toJsonString(appendable, key);
                    appendable.append(": ");
                    toJsonString(appendable, value);

                    if (i != writtenEntries.size() - 1) appendable.append(',');

                case Schema.Gap _:
                    appendable.append('\n');
            }
        }

        appendable.append("}\n");
    }

    private static void toJsonString(Appendable appendable, String str) throws IOException {
        appendable.append('"');

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '"' || c == '\\' || c <= '\u001F') {
                String escapeString = switch (c) {
                    case '\n' -> "\\n";
                    case '\t' -> "\\t";
                    case '\r' -> "\\r";
                    case '\f' -> "\\f";
                    case '\\' -> "\\";
                    case '"' -> "\\\"";
                    case '\b' -> "\\\b";
                    default -> "\\u%04X".formatted((int) c);
                };
                appendable.append(escapeString);
            } else {
                appendable.append(c);
            }
        }

        appendable.append('"');
    }
}
