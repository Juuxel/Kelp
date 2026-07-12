/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool;

import juuxel.translationtool.gui.TranslationToolWindow;
import juuxel.translationtool.gui.model.Project;
import juuxel.translationtool.gui.model.TranslationFile;
import juuxel.translationtool.gui.model.TranslationModel;
import juuxel.translationtool.io.TranslationFileWriter;
import juuxel.translationtool.util.Args;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class Main {
    static void main(String[] args) throws IOException {
        var fileArg = new Args.ArgsSchema.Positional("file");
        var reformatArg = new Args.ArgsSchema.Flag("--reformat");
        var argsSchema = new Args.ArgsSchema(
            List.of(fileArg),
            List.of(
                Args.ArgsSchema.HELP,
                Args.ArgsSchema.VERSION,
                reformatArg
            )
        );
        var parsedArgs = Args.parse(args, argsSchema);

        if (parsedArgs.shouldShowHelp()) {
            argsSchema.showHelp();
            System.exit(parsedArgs.exitCode());
        } else if (parsedArgs.shouldShowVersion()) {
            var version = Optional.ofNullable(Main.class.getModule().getDescriptor())
                .flatMap(ModuleDescriptor::rawVersion)
                .orElse("(unavailable version)");
            System.out.println("Translation Tool " + version);
            System.exit(parsedArgs.exitCode());
        }

        String filePath = parsedArgs.parsed().get(fileArg);

        if (parsedArgs.parsed().containsKey(reformatArg)) {
            if (filePath == null) {
                System.err.println("Must provide file path to reformat");
                System.exit(1);
            }

            reformat(Path.of(filePath));
            return;
        }

        SwingUtilities.invokeLater(() -> {
            var window = new TranslationToolWindow();
            window.setVisible(true);

            if (filePath != null) {
                window.openFromCli(filePath);
            }
        });
    }

    private static void reformat(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException("File " + path + " does not exist");
        }

        boolean dir = Files.isDirectory(path);
        TranslationModel model = dir ? Project.openDirectory(path) : TranslationFile.of(path);

        for (var file : model.getFiles().subList(1, model.getFiles().size())) {
            file.setSchema(model.getSchema());
        }

        for (var file : model.getFiles()) {
            try (var writer = Files.newBufferedWriter(file.getFilePath(), StandardCharsets.UTF_8)) {
                TranslationFileWriter.write(writer, file.translationsAsMap(), file.getSchema());
            }
        }
    }
}
