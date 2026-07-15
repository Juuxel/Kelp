/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import juuxel.translationtool.gui.model.TranslationFile;
import juuxel.translationtool.util.TriState;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SyncDialog extends JDialog {
    private final Action addAction = new SimpleAction("Sync", Icons.sync(), () -> {
        approved = true;
        dispose();
    });
    private boolean approved = false;

    private final List<String> keys;
    private final boolean[] enabled;
    private final TriStateCheckBox headerCheckBox = new TriStateCheckBox();
    private final JCheckBox applySchemaCheckBox = new JCheckBox("Apply Schema", true);

    public SyncDialog(JFrame owner, TranslationFile primary, TranslationFile currentFile) {
        super(owner, "Sync", true);

        keys = new ArrayList<>(primary.translationsAsMap().sequencedKeySet());
        keys.removeAll(currentFile.translationsAsMap().keySet());

        enabled = new boolean[keys.size()];
        Arrays.fill(enabled, true);
        headerCheckBox.setState(TriState.TRUE);

        var contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        var columnModel = new DefaultTableColumnModel();
        var enabledCol = new TableColumn(0, 10);
        var keyCol = new TableColumn(1, 300);
        keyCol.setHeaderValue("Key");
        columnModel.addColumn(enabledCol);
        columnModel.addColumn(keyCol);
        var header = new JTableHeader(columnModel) {
            {
                headerCheckBox.setBackground(getBackground());
                headerCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
                add(headerCheckBox);
            }

            @Override
            public void doLayout() {
                super.doLayout();
                headerCheckBox.setBounds(getHeaderRect(0));
            }
        };
        var tableModel = new TableModelImpl();
        headerCheckBox.addActionListener(_ -> {
            switch (headerCheckBox.getState()) {
                case TRUE -> Arrays.fill(enabled, true);
                case FALSE -> Arrays.fill(enabled, false);
                case INDETERMINATE -> {
                    return;
                }
            }

            tableModel.fireTableDataChanged();
            updateAddEnabled();
        });
        var table = new JTable(tableModel, columnModel);
        table.setTableHeader(header);
        contentPane.add(new JScrollPane(table));

        var buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(applySchemaCheckBox);
        buttonPanel.add(new JButton(new SimpleAction("Cancel", Icons.close(), this::dispose)));
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(new JButton(addAction));

        contentPane.add(buttonPanel);

        setContentPane(contentPane);

        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void updateHeaderCheckBox() {
        boolean trueEncountered = false;
        boolean falseEncountered = false;

        for (boolean b : enabled) {
            if (b) {
                trueEncountered = true;
            } else {
                falseEncountered = true;
            }
        }

        TriState selected;
        if (trueEncountered) {
            if (falseEncountered) {
                selected = TriState.INDETERMINATE;
            } else {
                selected = TriState.TRUE;
            }
        } else {
            selected = TriState.FALSE;
        }
        headerCheckBox.setState(selected);
    }

    private void updateAddEnabled() {
        addAction.setEnabled(!headerCheckBox.getState().isFalse());
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean shouldApplySchema() {
        return applySchemaCheckBox.isSelected();
    }

    public List<String> getKeysToAdd() {
        List<String> keysToAdd = new ArrayList<>(keys);

        for (int i = keysToAdd.size() - 1; i >= 0; i--) {
            if (!enabled[i]) keysToAdd.remove(i);
        }

        return keysToAdd;
    }

    private final class TableModelImpl extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return keys.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return switch (columnIndex) {
                case 0 -> enabled[rowIndex];
                case 1 -> keys.get(rowIndex);
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && aValue instanceof Boolean b) {
                enabled[rowIndex] = b;
                updateHeaderCheckBox();
                updateAddEnabled();
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;

            return Object.class;
        }
    }
}
