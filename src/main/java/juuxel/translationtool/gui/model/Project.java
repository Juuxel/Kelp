/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui.model;

import juuxel.translationtool.schema.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class Project implements TranslationModel {
    private static final Comparator<String> LANGUAGE_CODE_COMPARATOR = (a, b) -> {
        if ("en_us.json".equals(a)) {
            return "en_us.json".equals(b) ? 0 : -1;
        }

        return a.compareTo(b);
    };
    private final Path directory;
    private final List<TranslationFile> files;

    private Project(Path directory, List<TranslationFile> files) {
        this.directory = directory;
        this.files = files;
    }

    public static Project openDirectory(Path directory) throws IOException {
        try (var fileStream = Files.list(directory)) {
            var files = fileStream.collect(Collectors.toCollection(ArrayList::new));
            files.removeIf(p -> !Files.isRegularFile(p) || !p.getFileName().toString().endsWith(".json"));
            files.sort(
                Comparator.comparing(p -> p.getFileName().toString(), LANGUAGE_CODE_COMPARATOR)
            );

            if (files.isEmpty()) {
                throw new IOException("Can't select empty directory");
            }

            List<TranslationFile> translationFiles = new ArrayList<>(files.size());

            for (Path file : files) {
                translationFiles.add(TranslationFile.of(file));
            }

            return new Project(directory, translationFiles);
        }
    }

    @Override
    public List<TranslationFile> getFiles() {
        return files;
    }

    @Override
    public Schema getSchema() {
        return getPrimaryFile().getSchema();
    }

    @Override
    public String getDescription() {
        return directory.toString();
    }

    @Override
    public void renameKey(TranslationFile origin, String from, String to) {
        for (TranslationFile file : files) {
            file.renameKey(origin, from, to);
        }
    }

    @Override
    public void deleteRows(TranslationFile origin, List<String> keys) {
        for (TranslationFile file : files) {
            file.deleteRows(origin, keys);
        }
    }
}
