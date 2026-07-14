package juuxel.translationtool.gui;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

public final class Icons {
    private static final int ICON_SIZE = 16;
    private static final Icon[] ICONS = loadIcons();

    public static Icon add() {
        return ICONS[0];
    }

    public static Icon delete() {
        return ICONS[1];
    }

    public static Icon arrowUp() {
        return ICONS[2];
    }

    public static Icon arrowDown() {
        return ICONS[3];
    }

    public static Icon search() {
        return ICONS[4];
    }

    public static Icon undo() {
        return ICONS[5];
    }

    public static Icon redo() {
        return ICONS[6];
    }

    public static Icon gap() {
        return ICONS[7];
    }

    public static Icon atEnd() {
        return ICONS[8];
    }

    public static Icon close() {
        return ICONS[9];
    }

    public static Icon file() {
        return ICONS[10];
    }

    public static Icon project() {
        return ICONS[11];
    }

    public static Icon save() {
        return ICONS[12];
    }

    public static Icon batch() {
        return ICONS[13];
    }

    public static Icon key() {
        return ICONS[14];
    }

    public static Icon translation() {
        return ICONS[15];
    }

    private static Icon[] loadIcons() {
        try {
            BufferedImage fullImage;

            try (var in = Icons.class.getResourceAsStream("icons.png")) {
                fullImage = ImageIO.read(in);
            }

            int widthInIcons = fullImage.getWidth() / ICON_SIZE;
            int heightInIcons = fullImage.getHeight() / ICON_SIZE;
            Icon[] icons = new Icon[widthInIcons * heightInIcons];

            for (int row = 0; row < heightInIcons; row++) {
                for (int col = 0; col < widthInIcons; col++) {
                    var image = fullImage.getSubimage(col * ICON_SIZE, row * ICON_SIZE, ICON_SIZE, ICON_SIZE);
                    var icon = new ImageIcon(image);
                    icons[col + row * widthInIcons] = icon;
                }
            }

            return icons;
        } catch (Exception e) {
            throw new RuntimeException("Could not load icons", e);
        }
    }
}
