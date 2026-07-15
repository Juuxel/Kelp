/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Function;

public final class SearchableTable extends JPanel {
    private final JTable table;
    private final ColumnPredicate columnPredicate;
    private final JPanel searchBar = new JPanel(new BorderLayout());
    private final JTextField searchField = new JTextField();
    private final Action showSearchBarAction = new SimpleAction("Search", Icons.search(), this::showSearchBar);
    private final Action toggleSearchBarAction = new SimpleAction("Search", Icons.search(), this::toggleSearchBar);
    private String lastSearchTerm = "";
    private int lastSearchIndex;

    public SearchableTable(JTable table, Function<JTable, JScrollPane> scrollFactory) {
        this(table, scrollFactory, (_, _) -> true);
    }

    public SearchableTable(JTable table, Function<JTable, JScrollPane> scrollFactory, ColumnPredicate columnPredicate) {
        this.table = table;
        this.columnPredicate = columnPredicate;
        var scroll = scrollFactory.apply(table);
        searchBar.setVisible(false);

        searchField.addActionListener(_ -> search(false));

        var searchButtonPanel = new JPanel();
        searchButtonPanel.setLayout(new BoxLayout(searchButtonPanel, BoxLayout.X_AXIS));
        searchButtonPanel.add(createActionButton(new SimpleAction("Search Up", Icons.arrowUp(), () -> search(true))));
        searchButtonPanel.add(createActionButton(new SimpleAction("Search Down", Icons.arrowDown(), () -> search(false))));
        searchButtonPanel.add(createActionButton(new SimpleAction("Close", Icons.close(), () -> searchBar.setVisible(false))));

        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(searchButtonPanel, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(searchBar, BorderLayout.NORTH);
    }

    private static JButton createActionButton(Action action) {
        var button = new JButton(action);
        button.setHideActionText(true);
        return button;
    }

    private void search(boolean up) {
        var term = searchField.getText();

        if (term.isEmpty()) {
            table.clearSelection();
            return;
        }

        int start = term.equals(lastSearchTerm) ? (up ? lastSearchIndex : lastSearchIndex + 1) : 0;
        lastSearchTerm = term;

        outer: for (int rawRow = up ? table.getRowCount() - 1 : 0; up ? rawRow >= 0 : rawRow < table.getRowCount(); rawRow += up ? -1 : 1) {
            int row = (rawRow + start) % table.getRowCount(); // for wraparound

            for (int col = 0; col < table.getColumnCount(); col++) {
                if (!columnPredicate.testColumn(col, table.getModel().getColumnClass(col))) continue;
                var value = table.getModel().getValueAt(row, col);
                if (Objects.toString(value).contains(term)) {
                    table.changeSelection(row, col, false, false);
                    lastSearchIndex = row;
                    break outer;
                }
            }
        }
    }

    public void showSearchBar() {
        searchBar.setVisible(true);
        // invokeLater is needed because the search bar is hidden by default.
        // Focusing only works after the initial layout event, so we need to postpone this after that.
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);
    }

    public void toggleSearchBar() {
        if (!searchBar.isVisible()) {
            showSearchBar();
        } else {
            searchBar.setVisible(false);
        }
    }

    public Action getShowSearchBarAction() {
        return showSearchBarAction;
    }

    public Action getToggleSearchBarAction() {
        return toggleSearchBarAction;
    }

    public void addSearchActionKeyBinding(JComponent component) {
        var actionMap = component.getActionMap();
        var inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        actionMap.put("search", showSearchBarAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "search");
    }

    public interface ColumnPredicate {
        boolean testColumn(int index, Class<?> columnClass);
    }
}
