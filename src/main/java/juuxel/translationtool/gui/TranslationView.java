/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import juuxel.translationtool.gui.model.TranslationFile;
import juuxel.translationtool.util.Mth;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TranslationView {
    private final TranslationTool owner;
    private final TranslationFile file;
    private final TranslationFileTableModel tableModel;
    private final JTable table;
    private final JPanel mainView;

    private final Action addAboveAction;
    private final Action addBelowAction;
    private final Action addGapAboveAction;
    private final Action addGapBelowAction;
    private final Action removeAction;
    private final Action moveUpAction;
    private final Action moveDownAction;

    public TranslationView(TranslationTool owner, TranslationFile file) {
        this.owner = owner;
        this.file = file;
        this.mainView = new JPanel(new BorderLayout());

        var renderer = new TableCellRendererImpl();
        var columnModel = new DefaultTableColumnModel();
        var keyCol = new TableColumn(0, 75, renderer, null);
        keyCol.setHeaderValue("Key");
        keyCol.setHeaderRenderer(new HeaderRendererImpl(Icons.key()));
        var valueCol = new TableColumn(1, 300, renderer, null);
        valueCol.setHeaderValue("Translation");
        valueCol.setHeaderRenderer(new HeaderRendererImpl(Icons.translation()));
        columnModel.addColumn(keyCol);
        columnModel.addColumn(valueCol);
        this.tableModel = new TranslationFileTableModel();
        this.table = new JTable(tableModel, columnModel);
        table.setTableHeader(new JTableHeader(columnModel));
        table.getSelectionModel().addListSelectionListener(_ -> updateSelectionDependentActions());
        var searchableTable = new SearchableTable(table, t -> new JScrollPane(
            t,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        ));

        var toolBar = new JToolBar() {
            @Override
            protected JButton createActionComponent(Action a) {
                var button = super.createActionComponent(a);
                button.setHideActionText(false);
                return button;
            }
        };
        toolBar.setFloatable(false);
        addAboveAction = new SimpleAction("Above", Icons.arrowUp(), () -> addRow(AddPosition.BEFORE_SELECTION, new TranslationFile.Translation()));
        addBelowAction = new SimpleAction("Below", Icons.arrowDown(), () -> addRow(AddPosition.AFTER_SELECTION, new TranslationFile.Translation()));
        addGapAboveAction = new SimpleAction("Above", Icons.arrowUp(), () -> addRow(AddPosition.BEFORE_SELECTION, new TranslationFile.Gap()));
        addGapBelowAction = new SimpleAction("Below", Icons.arrowDown(), () -> addRow(AddPosition.AFTER_SELECTION, new TranslationFile.Gap()));
        removeAction = new SimpleAction("Delete", Icons.delete(), this::deleteSelectedRows);
        moveUpAction = new SimpleAction("Move Up", Icons.arrowUp(), () -> moveSelectedRow(true));
        moveDownAction = new SimpleAction("Move Down", Icons.arrowDown(), () -> moveSelectedRow(false));
        updateUndoRedoEnabled();
        updateSelectionDependentActions();
        var addMenu = new JPopupMenu("Add");
        addMenu.add(addAboveAction);
        addMenu.add(addBelowAction);
        addMenu.add(new SimpleAction("At End", Icons.atEnd(), () -> addRow(AddPosition.AT_END, new TranslationFile.Translation())));
        addMenu.add(new SimpleAction("Batch", Icons.batch(), () -> {
            var dialog = new BatchAddDialog(owner.getApp().getFrame());
            dialog.setVisible(true);

            if (dialog.isApproved()) {
                file.createSnapshot();
                try {
                    file.addBatch(dialog.getInputs());
                    tableModel.fireTableDataChanged();
                    owner.markDirty();
                    updateUndoRedoEnabled();
                    updateSelectionDependentActions();
                } catch (Exception e) {
                    e.printStackTrace();
                    owner.getApp().showErrorPopup(e);
                    file.dropSnapshot();
                }
            }
        }));
        var addGapMenu = new JPopupMenu("Add Gap");
        addGapMenu.add(addGapAboveAction);
        addGapMenu.add(addGapBelowAction);
        addGapMenu.add(new SimpleAction("At End", Icons.atEnd(), () -> addRow(AddPosition.AT_END, new TranslationFile.Gap())));
        toolBar.add(createPopupMenuButton(addMenu, Icons.add()));
        toolBar.add(createPopupMenuButton(addGapMenu, Icons.gap()));
        toolBar.add(removeAction);
        toolBar.addSeparator();
        toolBar.add(moveUpAction);
        toolBar.add(moveDownAction);
        toolBar.addSeparator();
        toolBar.add(searchableTable.getToggleSearchBarAction());

        var actionMap = mainView.getActionMap();
        var inputMap = mainView.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        actionMap.put("search", searchableTable.getShowSearchBarAction());
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "search");

        if (file != owner.getModel().getPrimaryFile()) {
            toolBar.addSeparator();
            toolBar.add(new SimpleAction("Sync", Icons.sync(), () -> {
                var dialog = new SyncDialog(owner.getApp().getFrame(), owner.getModel().getPrimaryFile(), file);
                dialog.setVisible(true);

                if (dialog.isApproved()) {
                    file.createSnapshot();

                    var rowsToAdd =
                        dialog.getKeysToAdd().stream()
                            .map(key -> new TranslationFile.Translation(key, ""))
                            .toList();

                    file.getRows().addAll(rowsToAdd);

                    if (dialog.shouldApplySchema()) {
                        file.applySchema(owner.getModel().getSchema(), false);
                    }

                    tableModel.fireTableDataChanged();
                    table.clearSelection();

                    for (var row : rowsToAdd) {
                        int index = file.getRows().indexOf(row);
                        table.addRowSelectionInterval(index, index);
                    }

                    owner.markDirty();
                    updateUndoRedoEnabled();
                    updateSelectionDependentActions();
                }
            }));
            toolBar.add(new SimpleAction("Apply Schema", Icons.brush(), () -> {
                file.createSnapshot();
                file.applySchema(owner.getModel().getSchema(), false);
                tableModel.fireTableDataChanged();
                table.clearSelection();
                owner.markDirty();
                updateUndoRedoEnabled();
                updateSelectionDependentActions();
            }));
        }

        mainView.add(searchableTable, BorderLayout.CENTER);
        mainView.add(toolBar, BorderLayout.PAGE_START);
    }

    private static JButton createPopupMenuButton(JPopupMenu menu, Icon icon) {
        var button = new JButton(menu.getLabel(), icon);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.addActionListener(_ -> menu.show(button, button.getX(), button.getY() + button.getHeight()));
        return button;
    }

    public JComponent asComponent() {
        return mainView;
    }

    public void whenSelected() {
        updateUndoRedoEnabled();
    }

    public void undo() {
        var snapshot = file.undo();
        tableModel.fireTableDataChanged();
        updateUndoRedoEnabled();
        if (snapshot != null) refreshTableModelFromSnapshot(snapshot);
    }

    public void redo() {
        var snapshot = file.redo();
        tableModel.fireTableDataChanged();
        updateUndoRedoEnabled();
        if (snapshot != null) refreshTableModelFromSnapshot(snapshot);
    }

    private void updateSelectionDependentActions() {
        int[] rows = table.getSelectedRows();

        switch (rows.length) {
            case 0 -> {
                addAboveAction.setEnabled(false);
                addBelowAction.setEnabled(false);
                addGapAboveAction.setEnabled(false);
                addGapBelowAction.setEnabled(false);
                removeAction.setEnabled(false);
                moveUpAction.setEnabled(false);
                moveDownAction.setEnabled(false);
            }

            case 1 -> {
                addAboveAction.setEnabled(true);
                addBelowAction.setEnabled(true);
                addGapAboveAction.setEnabled(true);
                addGapBelowAction.setEnabled(true);
                removeAction.setEnabled(true);
            }

            default -> {
                addAboveAction.setEnabled(false);
                addBelowAction.setEnabled(false);
                addGapAboveAction.setEnabled(false);
                addGapBelowAction.setEnabled(false);
                removeAction.setEnabled(true);
            }
        }

        if (Mth.isIncreasingInterval(rows)) {
            int start = rows[0];
            int endExclusive = start + rows.length;
            moveUpAction.setEnabled(start > 0);
            moveDownAction.setEnabled(endExclusive < table.getRowCount());
        } else {
            moveUpAction.setEnabled(false);
            moveDownAction.setEnabled(false);
        }
    }

    private void updateUndoRedoEnabled() {
        owner.getApp().setUndoRedoEnabled(file.canUndo(), file.canRedo());
    }

    private void deleteSelectedRows() {
        file.createSnapshot();
        List<String> keysRemoved = new ArrayList<>();
        int[] selectedRows = table.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            var row = file.getRows().remove(selectedRows[i]);

            if (row instanceof TranslationFile.Translation translation) {
                keysRemoved.add(translation.getKey());
            }
        }

        if (!keysRemoved.isEmpty()) {
            owner.getModel().deleteRows(file, keysRemoved);
        }

        owner.markDirty();
        tableModel.fireTableDataChanged();
        updateSelectionDependentActions();

        for (TranslationTool.FileListEntry entry : owner.getFileListEntries()) {
            entry.view().updateUndoRedoEnabled();
        }
    }

    private void moveSelectedRow(boolean up) {
        int[] selected = table.getSelectedRows();
        if (!Mth.isIncreasingInterval(selected)) throw new IllegalStateException("Can only move contiguous set of rows");

        int start = selected[0];
        int endExclusive = start + selected.length;

        if (up) {
            if (start > 0) {
                file.createSnapshot();

                var neighbor = file.getRows().remove(start - 1);
                file.getRows().add(endExclusive - 1, neighbor);

                owner.markDirty();
                updateUndoRedoEnabled();
                tableModel.fireTableDataChanged();
                table.getSelectionModel().setSelectionInterval(start - 1, endExclusive - 2);
                updateSelectionDependentActions();
            }
        } else {
            if (endExclusive < table.getRowCount()) {
                file.createSnapshot();

                var neighbor = file.getRows().remove(endExclusive);
                file.getRows().add(start, neighbor);

                owner.markDirty();
                updateUndoRedoEnabled();
                tableModel.fireTableDataChanged();
                table.getSelectionModel().setSelectionInterval(start + 1, endExclusive);
                updateSelectionDependentActions();
            }
        }
    }

    private void refreshTableModelFromSnapshot(TranslationFile.Snapshot snapshot) {
        // TODO: WHY AREN'T YOU WORKING!?!?? :p
        for (var row : snapshot.getModifiedRows()) {
            int index = file.getRows().indexOf(row);
            tableModel.fireTableCellUpdated(index, 0); // key
            tableModel.fireTableCellUpdated(index, 1); // translation
        }
    }

    private void addRow(AddPosition position, TranslationFile.Row row) {
        file.createSnapshot();

        int selected = table.getSelectedRow();
        int index = switch (position) {
            case BEFORE_SELECTION -> selected;
            case AFTER_SELECTION -> selected + 1;
            case AT_END -> table.getRowCount();
        };
        file.getRows().add(index, row);

        owner.markDirty();
        updateUndoRedoEnabled();
        updateSelectionDependentActions();
        tableModel.fireTableRowsInserted(index, index);
        table.changeSelection(index, 0, false, false);
    }

    private final class TranslationFileTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return file.getSchema().entries().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            var row = file.getRows().get(rowIndex);
            return switch (row) {
                case TranslationFile.Translation translation -> columnIndex == 0 ? translation.getKey() : translation.getValue();
                case TranslationFile.Gap gap -> gap;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return file.getRows().get(rowIndex) instanceof TranslationFile.Translation;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            var row = file.getRows().get(rowIndex);
            if (!(row instanceof TranslationFile.Translation translation)) return;
            var newContent = Objects.toString(aValue);

            if (columnIndex == 0) {
                var oldKey = translation.getKey();
                if (!newContent.equals(oldKey)) {
                    var snapshot = file.createSnapshot();
                    translation.setKey(newContent);
                    snapshot.getModifiedRows().add(translation);
                    owner.getModel().renameKey(file, oldKey, newContent);
                    owner.markDirty();

                    for (TranslationTool.FileListEntry entry : owner.getFileListEntries()) {
                        entry.view().updateUndoRedoEnabled();
                    }
                }
            } else {
                if (!newContent.equals(translation.getValue())) {
                    var snapshot = file.createSnapshot();
                    translation.setValue(newContent);
                    snapshot.getModifiedRows().add(translation);
                    owner.markDirty();
                    updateUndoRedoEnabled();
                }
            }
        }
    }

    private static final class TableCellRendererImpl extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof TranslationFile.Gap) {
                setText("<html><i>Gap</i>");
            }

            return this;
        }
    }

    private static final class HeaderRendererImpl extends DefaultTableCellRenderer {
        private final Icon icon;

        HeaderRendererImpl(Icon icon) {
            this.icon = icon;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            var header = table.getTableHeader();
            setBackground(header.getBackground());
            setForeground(header.getForeground());
            setFont(header.getFont().deriveFont(Font.BOLD));
            setIcon(icon);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    private enum AddPosition {
        BEFORE_SELECTION,
        AFTER_SELECTION,
        AT_END,
    }
}
