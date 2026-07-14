/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import juuxel.translationtool.gui.model.Project;
import juuxel.translationtool.gui.model.TranslationFile;
import juuxel.translationtool.gui.model.TranslationModel;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TranslationToolWindow extends JFrame {
    private static final String TITLE = "Kelp - ";
    private final JFileChooser fileChooser = new JFileChooser();
    private final JComponent emptyProjectView = new JLabel("No project opened");
    private final List<Action> projectDependentActions;
    private TranslationTool translationTool = null;

    public TranslationToolWindow() {
        // Set up menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        var saveProjectAction = new SimpleAction("Save Project", Icons.save(), () -> {
            try {
                saveProject();
            } catch (IOException e) {
                e.printStackTrace();
                showErrorPopup(e);
            }
        });
        var saveProjectItem = new JMenuItem(saveProjectAction);
        saveProjectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        var closeProjectAction = new SimpleAction("Close Project", Icons.close(), this::closeProject);
        fileMenu.add(new SimpleAction("Open Project", Icons.project(), this::openProject));
        fileMenu.add(new SimpleAction("Open Single File", Icons.file(), this::openFile));
        fileMenu.add(saveProjectItem);
        fileMenu.add(closeProjectAction);
        projectDependentActions = List.of(saveProjectAction, closeProjectAction);
        setProjectDependentActionsEnabled(false);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        setContentPane(emptyProjectView);

        // Set up window state
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        refreshTitle();
        setSize(640, 480);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (closeProject()) {
                    dispose();
                }
            }
        });
    }

    public void openFromCli(String filePath) {
        try {
            var path = Path.of(filePath);
            boolean dir = Files.isDirectory(path);
            var model = dir ? Project.openDirectory(path) : TranslationFile.of(path);
            fileChooser.setSelectedFile(path.toFile());
            setModel(model);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPopup(e);
        }
    }

    private void openProject() {
        if (!closeProject()) return;
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setFileFilter(null);
        int option = fileChooser.showOpenDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) return;

        try {
            var model = Project.openDirectory(fileChooser.getSelectedFile().toPath());
            setModel(model);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPopup(e);
        }
    }

    private void openFile() {
        if (!closeProject()) return;
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".json");
            }

            @Override
            public String getDescription() {
                return "JSON files (.json)";
            }
        });
        int option = fileChooser.showOpenDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) return;

        try {
            var model = TranslationFile.of(fileChooser.getSelectedFile().toPath());
            setModel(model);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPopup(e);
        }
    }

    private void setModel(TranslationModel model) {
        translationTool = new TranslationTool(this, model);
        setContentPane(translationTool.getMainView());
        setProjectDependentActionsEnabled(true);
        revalidate();
        refreshTitle();
    }

    public void showErrorPopup(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "An error occurred", JOptionPane.ERROR_MESSAGE);
    }

    private void saveProject() throws IOException {
        if (translationTool != null) {
            translationTool.save();
        }
    }

    private boolean closeProject() {
        if (translationTool == null) return true;

        if (translationTool.isDirty()) {
            int option = JOptionPane.showConfirmDialog(
                this,
                "This project has unsaved changes. Do you want to save?",
                "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION
            );

            switch (option) {
                case JOptionPane.YES_OPTION -> {
                    try {
                        translationTool.save();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showErrorPopup(e);
                        return false;
                    }
                }

                case JOptionPane.CANCEL_OPTION -> {
                    return false;
                }
            }
        }

        translationTool = null;
        setContentPane(emptyProjectView);
        revalidate();
        refreshTitle();
        setProjectDependentActionsEnabled(false);
        return true;
    }

    private void setProjectDependentActionsEnabled(boolean enabled) {
        for (var action : projectDependentActions) {
            action.setEnabled(enabled);
        }
    }

    public void refreshTitle() {
        setTitle(TITLE + (translationTool != null ? translationTool.stateToString() : "unopened"));
    }
}
