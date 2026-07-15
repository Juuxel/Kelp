/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui.model;

import juuxel.translationtool.io.TranslationFileReader;
import juuxel.translationtool.schema.Schema;
import juuxel.translationtool.util.CartesianProduct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TranslationFile implements TranslationModel {
    private final Path filePath;
    private List<Row> rows = new ArrayList<>();
    private final List<Snapshot> history = new ArrayList<>();
    private int positionInHistory = 0;

    private TranslationFile(Path filePath, Map<String, String> translations, Schema schema) {
        this.filePath = filePath;

        for (var entry : schema.entries()) {
            switch (entry) {
                case Schema.Key(var key) -> rows.add(new Translation(key, translations.get(key)));
                case Schema.Gap _ -> rows.add(new Gap());
            }
        }

        history.add(new Snapshot(rows));
    }

    public static TranslationFile of(Path path) throws IOException {
        try (var reader = new TranslationFileReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            var readResult = reader.read();
            return new TranslationFile(path, readResult.translations(), readResult.schema());
        }
    }

    public Path getFilePath() {
        return filePath;
    }

    public List<Row> getRows() {
        return rows;
    }

    @Override
    public List<TranslationFile> getFiles() {
        return List.of(this);
    }

    public SequencedMap<String, String> translationsAsMap() {
        var translations = new LinkedHashMap<String, String>();

        for (Row row : rows) {
            if (row instanceof Translation translation) {
                translations.putIfAbsent(translation.key, translation.value);
            }
        }

        return translations;
    }

    @Override
    public Schema getSchema() {
        var entries = new ArrayList<Schema.Entry>();

        for (Row row : rows) {
            switch (row) {
                case Translation translation -> entries.add(new Schema.Key(translation.key));
                case Gap _ -> entries.add(Schema.GAP);
            }
        }

        return new Schema(entries);
    }

    public void applySchema(Schema schema, boolean logRemovals) {
        Map<String, Translation> translations =
            rows.stream()
                .mapMulti((Row row, Consumer<Translation> sink) -> {
                    if (row instanceof Translation translation) {
                        sink.accept(translation);
                    }
                })
                .collect(Collectors.toMap(Translation::getKey, Function.identity()));
        Set<String> remainingKeys = logRemovals ? new HashSet<>(translations.keySet()) : null;
        rows.clear();

        // Filters out duplicated and leading gaps
        boolean shouldAddGap = false;

        for (var entry : schema.entries()) {
            switch (entry) {
                case Schema.Key(var key) -> {
                    var translation = translations.get(key);
                    if (translation != null) {
                        rows.add(translation);
                        shouldAddGap = true;
                        if (logRemovals) remainingKeys.remove(key);
                    }
                }

                case Schema.Gap _ -> {
                    if (shouldAddGap) {
                        rows.add(new Gap());
                        shouldAddGap = false;
                    }
                }
            }
        }

        // Remove trailing gaps
        while (!rows.isEmpty() && rows.getLast() instanceof Gap) {
            rows.removeLast();
        }

        if (logRemovals && !remainingKeys.isEmpty()) {
            System.err.printf("Removing %d keys from %s:%n", remainingKeys.size(), filePath);

            for (String key : remainingKeys) {
                System.err.println(" - " + key);
            }
        }
    }

    @Override
    public String getDescription() {
        return filePath.toString();
    }

    public Snapshot createSnapshot() {
        // Delete trailing history
        if (positionInHistory < history.size() - 1) {
            history.subList(positionInHistory, history.size()).clear();
        }

        var newRows = new ArrayList<>(rows);
        var snapshot = new Snapshot(newRows);
        history.add(snapshot);
        rows = newRows;
        positionInHistory++;
        return snapshot;
    }

    public Snapshot getCurrentSnapshot() {
        return history.get(positionInHistory);
    }

    public void dropSnapshot() {
        if (!canUndo() || positionInHistory != history.size() - 1)
            throw new IllegalStateException("Can only drop freshly created snapshot");
        undo();
        history.removeLast();
    }

    public boolean canUndo() {
        return positionInHistory > 0;
    }

    public Snapshot undo() {
        if (!canUndo()) return null;
        var oldSnapshot = getCurrentSnapshot();
        positionInHistory--;
        rows = history.get(positionInHistory).rows;
        return oldSnapshot;
    }

    public boolean canRedo() {
        return positionInHistory < history.size() - 1;
    }

    public Snapshot redo() {
        if (!canRedo()) return null;
        var oldSnapshot = getCurrentSnapshot();
        positionInHistory++;
        rows = history.get(positionInHistory).rows;
        return oldSnapshot;
    }

    @Override
    public void renameKey(TranslationFile origin, String from, String to) {
        if (origin != this) {
            createSnapshot();
            boolean modified = false;

            for (Row row : rows) {
                if (row instanceof Translation translation && translation.key.equals(from)) {
                    translation.setKey(to);
                    modified = true;
                }
            }

            if (!modified) dropSnapshot();
        }
    }

    @Override
    public void deleteRows(TranslationFile origin, List<String> keys) {
        if (origin != this) {
            createSnapshot();
            boolean removed = rows.removeIf(row -> row instanceof Translation translation && keys.contains(translation.key));
            if (!removed) dropSnapshot();
        }
    }

    public void addBatch(BatchAddInputs inputs) {
        var instances = inputs.fields().stream().map(BatchAddInputs.Field::instances).toList();
        var combos = CartesianProduct.of(instances);
        var instancesForGrouping = CartesianProduct.of(
            inputs.fields()
                .stream()
                .filter(BatchAddInputs.Field::group)
                .map(BatchAddInputs.Field::instances)
                .toList()
        );

        Map<String, String> keySubstitutions = new HashMap<>();
        Map<String, String> translationSubstitutions = new HashMap<>();

        for (List<BatchAddInputs.FieldInstance> group : instancesForGrouping) {
            int pos = -1;

            for (List<BatchAddInputs.FieldInstance> rowData : combos) {
                if (!rowData.containsAll(group)) continue;

                for (BatchAddInputs.FieldInstance fieldInstance : rowData) {
                    keySubstitutions.put(fieldInstance.name(), fieldInstance.key());
                    translationSubstitutions.put(fieldInstance.name(), fieldInstance.translation());
                }

                // compute pos for first
                if (pos < 0) {
                    if (inputs.insertAfterKeyTemplate().isEmpty()) {
                        pos = rows.size();
                    } else {
                        String expected = applyTemplate(inputs.insertAfterKeyTemplate(), keySubstitutions);
                        int insertAfterIndex = -1;

                        for (int i = 0; i < rows.size(); i++) {
                            Row row = rows.get(i);
                            if (row instanceof Translation translation && expected.equals(translation.getKey())) {
                                insertAfterIndex = i;
                                break;
                            }
                        }

                        pos = insertAfterIndex >= 0 ? insertAfterIndex + 1 : rows.size();
                    }
                }

                var key = applyTemplate(inputs.keyTemplate(), keySubstitutions);
                var translation = applyTemplate(inputs.translationTemplate(), translationSubstitutions);
                rows.add(pos++, new Translation(key, translation));
            }
        }
    }

    private static String applyTemplate(String template, Map<String, String> substitutions) {
        for (Map.Entry<String, String> entry : substitutions.entrySet()) {
            template = template.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        return template;
    }

    // Note: each Row instance is unique to one row in one translation file.
    public sealed interface Row {
    }

    public static final class Translation implements Row {
        private String key;
        private String value;

        public Translation() {
            this("", "");
        }

        public Translation(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static final class Gap implements Row {
    }

    public static final class Snapshot {
        private final List<Row> rows;
        private final Set<Row> modifiedRows = Collections.newSetFromMap(new IdentityHashMap<>());

        public Snapshot(List<Row> rows) {
            this.rows = rows;
        }

        /// @return the rows whose contents were modified
        public Set<Row> getModifiedRows() {
            return modifiedRows;
        }
    }
}
