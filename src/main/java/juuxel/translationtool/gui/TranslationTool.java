/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import juuxel.translationtool.gui.model.TranslationFile;
import juuxel.translationtool.gui.model.TranslationModel;
import juuxel.translationtool.io.TranslationFileWriter;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class TranslationTool {
    private final App app;
    private final List<FileListEntry> fileListEntries;
    private final JList<FileListEntry> fileList;
    private final JPanel mainView;

    private final TranslationModel model;
    private boolean dirty;
    private TranslationView selectedView;

    public TranslationTool(App app, TranslationModel model) {
        this.app = app;
        this.model = model;

        var files =
            model.getFiles()
                .stream()
                .map(file -> new FileListEntry(file, new TranslationView(this, file)))
                .toArray(FileListEntry[]::new);
        this.fileListEntries = List.of(files);
        this.fileList = new JList<>(files);
        setSelectedView(files[0].view);
        var listScrollPane = new JScrollPane(
            fileList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        var cards = new CardLayout();
        var central = new JPanel(cards);

        for (var entry : files) {
            central.add(entry.view.asComponent(), entry.toString());
        }

        this.mainView = new JPanel(new BorderLayout());
        mainView.add(central, BorderLayout.CENTER);
        mainView.add(listScrollPane, BorderLayout.LINE_START);

        fileList.addListSelectionListener(_ -> {
            cards.show(central, fileList.getSelectedValue().toString());
            setSelectedView(fileListEntries.get(fileList.getSelectedIndex()).view);
        });

        // Make the file list automatically shrink to fit when opened.
        SwingUtilities.invokeLater(mainView::revalidate);
    }

    private void setSelectedView(TranslationView selectedView) {
        this.selectedView = selectedView;
        selectedView.whenSelected();
    }

    public App getApp() {
        return app;
    }

    public TranslationView getSelectedView() {
        return selectedView;
    }

    public TranslationModel getModel() {
        return model;
    }

    public JComponent getMainView() {
        return mainView;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
        app.refreshTitle();
    }

    public String stateToString() {
        return dirty ? "*" + model.getDescription() : model.getDescription();
    }

    public void save() throws IOException {
        if (!dirty) return;

        for (TranslationFile file : model.getFiles()) {
            try (var writer = Files.newBufferedWriter(file.getFilePath(), StandardCharsets.UTF_8)) {
                TranslationFileWriter.write(writer, file.translationsAsMap(), file.getSchema());
            }
        }

        dirty = false;
        app.refreshTitle();
    }

    public List<FileListEntry> getFileListEntries() {
        return fileListEntries;
    }

    public record FileListEntry(TranslationFile file, TranslationView view) {
        @Override
        public String toString() {
            return file.getFilePath().getFileName().toString();
        }
    }
}
