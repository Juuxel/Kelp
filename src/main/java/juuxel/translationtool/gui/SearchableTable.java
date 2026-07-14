package juuxel.translationtool.gui;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.Function;

public final class SearchableTable extends JPanel {
    private final JTable table;
    private final JPanel searchBar = new JPanel(new BorderLayout());
    private final JTextField searchField = new JTextField();
    private final Action showSearchBarAction = new SimpleAction("Search", this::showSearchBar);
    private String lastSearchTerm = "";
    private int lastSearchIndex;

    public SearchableTable(JTable table, Function<JTable, JScrollPane> scrollFactory) {
        this.table = table;
        var scroll = scrollFactory.apply(table);
        var closeButton = new JButton("Close");
        closeButton.addActionListener(_ -> {
            searchBar.setVisible(false);
        });
        searchBar.setVisible(false);

        searchField.addActionListener(_ -> search());

        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(closeButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(searchBar, BorderLayout.NORTH);
    }

    private void search() {
        var term = searchField.getText();

        if (term.isEmpty()) {
            table.clearSelection();
            return;
        }

        int start = term.equals(lastSearchTerm) ? lastSearchIndex + 1 : 0;
        lastSearchTerm = term;

        outer: for (int rawRow = 0; rawRow < table.getRowCount(); rawRow++) {
            int row = (rawRow + start) % table.getRowCount(); // for wraparound

            for (int col = 0; col < table.getColumnCount(); col++) {
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

    public Action getShowSearchBarAction() {
        return showSearchBarAction;
    }
}
