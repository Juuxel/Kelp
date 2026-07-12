/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.io;

import juuxel.translationtool.schema.Schema;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TranslationFileReader implements Closeable {
    private final Map<String, String> translations = new HashMap<>();
    private final List<Schema.Entry> schema = new ArrayList<>();
    private final BufferedReader reader;
    private State state = State.START;
    private String currentKey;

    public TranslationFileReader(BufferedReader reader) {
        this.reader = reader;
    }

    public ReadResult read() throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            readLine(line);
        }

        return new ReadResult(Map.copyOf(translations), buildSchema());
    }

    private void readLine(String line) throws IOException {
        while (line.startsWith(" ") || line.startsWith("\n") || line.startsWith("\r") || line.startsWith("\t")) {
            line = line.substring(1);
        }

        if (line.isEmpty()) {
            if (state == State.EXPECTING_KEY) {
                schema.add(Schema.GAP);
            }

            return;
        }

        switch (state) {
            case START -> {
                if (!line.startsWith("{")) {
                    throw new IOException("JSON object must start with {");
                }

                state = State.EXPECTING_KEY;

                var next = line.substring(1);
                if (!next.isEmpty()) readLine(next);
            }

            case EXPECTING_KEY -> {
                var read = readJsonString(line);
                currentKey = read.content;
                schema.add(new Schema.Key(currentKey));
                state = State.EXPECTING_COLON;
                var next = line.substring(read.lengthInSource);
                if (!next.isEmpty()) readLine(next);
            }

            case EXPECTING_COLON -> {
                if (!line.startsWith(":")) {
                    throw new IOException("Expecting : between key and value");
                }

                state = State.EXPECTING_VALUE;
                var next = line.substring(1);
                if (!next.isEmpty()) readLine(next);
            }

            case EXPECTING_VALUE -> {
                var read = readJsonString(line);
                translations.put(currentKey, read.content);
                state = State.EXPECTING_COMMA_OR_EOF;
                var next = line.substring(read.lengthInSource);
                if (!next.isEmpty()) readLine(next);
            }

            case EXPECTING_COMMA_OR_EOF -> {
                if (line.startsWith(",")) {
                    state = State.EXPECTING_KEY;
                    var next = line.substring(1);
                    if (!next.isEmpty()) readLine(next);
                } else if (line.startsWith("}")) {
                    state = State.EOF;
                    var next = line.substring(1);
                    if (!next.isEmpty()) readLine(next);
                } else {
                    throw new IOException("Expecting , or } after entry");
                }
            }

            case EOF -> {
                throw new IOException("Content after EOF");
            }
        }
    }

    private JsonString readJsonString(String line) throws IOException {
        if (!line.startsWith("\"")) {
            throw new IOException("Expected JSON string");
        }

        enum StringState {
            NORMAL,
            IN_ESCAPE,
            IN_HEX_ESCAPE_1,
            IN_HEX_ESCAPE_2,
            IN_HEX_ESCAPE_3,
            IN_HEX_ESCAPE_4,
        }

        var hexEscapeBuffer = new StringBuilder(4);
        var contentBuilder = new StringBuilder();
        var state = StringState.NORMAL;

        for (int i = 1; i < line.length(); i++) {
            char c = line.charAt(i);

            switch (state) {
                case NORMAL -> {
                    if (c == '\\') {
                        state = StringState.IN_ESCAPE;
                    } else if (c == '"') {
                        return new JsonString(contentBuilder.toString(), i + 1);
                    } else if (c <= '\u001F') {
                        throw new IOException("Unescaped control character");
                    } else {
                        contentBuilder.append(c);
                    }
                }

                case IN_ESCAPE -> {
                    switch (c) {
                        case '"', '\\', '/' -> contentBuilder.append(c);
                        case 'b' -> contentBuilder.append('\b');
                        case 'f' -> contentBuilder.append('\f');
                        case 'n' -> contentBuilder.append('\n');
                        case 'r' -> contentBuilder.append('\r');
                        case 't' -> contentBuilder.append('\t');
                        case 'u' -> {
                            state = StringState.IN_HEX_ESCAPE_1;
                            continue;
                        }
                        default -> throw new IOException("Unknown escape character " + c);
                    }

                    state = StringState.NORMAL;
                }

                case IN_HEX_ESCAPE_1, IN_HEX_ESCAPE_2, IN_HEX_ESCAPE_3, IN_HEX_ESCAPE_4 -> {
                    if ('0' <= c && c <= '9' || 'A' <= c && c <= 'F' || 'a' <= c && c <= 'f') {
                        hexEscapeBuffer.append(c);

                        switch (state) {
                            case IN_HEX_ESCAPE_1 -> state = StringState.IN_HEX_ESCAPE_2;
                            case IN_HEX_ESCAPE_2 -> state = StringState.IN_HEX_ESCAPE_3;
                            case IN_HEX_ESCAPE_3 -> state = StringState.IN_HEX_ESCAPE_4;
                            case IN_HEX_ESCAPE_4 -> {
                                int parsed = Integer.parseInt(hexEscapeBuffer, 0, 4, 16);
                                contentBuilder.append((char) parsed);
                                hexEscapeBuffer.setLength(0);
                                state = StringState.NORMAL;
                            }
                        }
                    } else {
                        throw new IOException("Invalid character " + c + " in Unicode escape");
                    }
                }
            }
        }

        throw new IOException("Unclosed string");
    }

    private Schema buildSchema() {
        return new Schema(List.copyOf(schema));
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public record ReadResult(Map<String, String> translations, Schema schema) {
    }

    private record JsonString(String content, int lengthInSource) {
    }

    private enum State {
        START,
        EXPECTING_KEY,
        EXPECTING_COLON,
        EXPECTING_VALUE,
        EXPECTING_COMMA_OR_EOF,
        EOF,
    }
}
