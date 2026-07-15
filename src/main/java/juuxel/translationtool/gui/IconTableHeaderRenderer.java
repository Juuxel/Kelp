/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;

public final class IconTableHeaderRenderer extends DefaultTableCellRenderer {
    private final Icon icon;

    public IconTableHeaderRenderer(Icon icon) {
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
