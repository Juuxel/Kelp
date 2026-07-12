/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import juuxel.translationtool.gui.model.BatchAddInputs;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public final class BatchAddDialog extends JDialog {
    private final JPanel fieldContainer = new JPanel();
    private final List<FieldView> fields = new ArrayList<>();
    private final JTextField keyField = new JTextField();
    private final JTextField translationField = new JTextField();
    private final JTextField sortKeyField = new JTextField();
    private final Action addAction = new SimpleAction("Add", () -> {
        approved = true;
        dispose();
    });
    private boolean approved = false;

    public BatchAddDialog(JFrame owner) {
        super(owner, "Add Batch", true);

        var contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        fieldContainer.setLayout(new GridLayout(0, 1, 0, 5));
        addField();

        var addFieldButton = new JButton("Add");
        addFieldButton.addActionListener(_ -> addField());
        var fieldTitle = new JPanel();
        fieldTitle.setLayout(new BoxLayout(fieldTitle, BoxLayout.X_AXIS));
        fieldTitle.add(new JLabel("Fields"));
        fieldTitle.add(Box.createHorizontalStrut(5));
        fieldTitle.add(addFieldButton);
        fieldTitle.add(Box.createHorizontalGlue());

        var fieldScroll = new JScrollPane(fieldContainer);
        fieldScroll.setBorder(BorderFactory.createLoweredSoftBevelBorder());

        contentPane.add(fieldTitle);
        contentPane.add(fieldScroll);

        var keyLabel = new JLabel("Key");
        var translationLabel = new JLabel("Translation");
        var sortKeyLabel = new JLabel("Insert After...");
        var docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onUpdate();
            }

            private void onUpdate() {
                updateAddEnabled();
            }
        };
        keyField.getDocument().addDocumentListener(docListener);
        translationField.getDocument().addDocumentListener(docListener);
        sortKeyField.getDocument().addDocumentListener(docListener);
        var textFieldContainer = new JPanel();
        var textFieldContainerLayout = new GroupLayout(textFieldContainer);
        textFieldContainer.setLayout(textFieldContainerLayout);

        textFieldContainerLayout.setHorizontalGroup(
            textFieldContainerLayout.createSequentialGroup()
                .addGroup(textFieldContainerLayout.createParallelGroup().addComponent(keyLabel).addComponent(translationLabel).addComponent(sortKeyLabel))
                .addGap(5)
                .addGroup(textFieldContainerLayout.createParallelGroup().addComponent(keyField).addComponent(translationField).addComponent(sortKeyField))
        );
        textFieldContainerLayout.setVerticalGroup(
            textFieldContainerLayout.createSequentialGroup()
                .addGroup(textFieldContainerLayout.createParallelGroup().addComponent(keyLabel).addComponent(keyField))
                .addGroup(textFieldContainerLayout.createParallelGroup().addComponent(translationLabel).addComponent(translationField))
                .addGroup(textFieldContainerLayout.createParallelGroup().addComponent(sortKeyLabel).addComponent(sortKeyField))
        );

        contentPane.add(textFieldContainer);

        var buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(new JButton(new SimpleAction("Cancel", this::dispose)));
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(new JButton(addAction));

        contentPane.add(buttonPanel);

        setContentPane(contentPane);

        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void addField() {
        var fieldView = new FieldView();
        fields.add(fieldView);
        fieldView.deleteButton.addActionListener(_ -> {
            fieldContainer.remove(fieldView);
            fields.remove(fieldView);
            updateAddEnabled();
            revalidate();
        });
        fieldContainer.add(fieldView);
        updateAddEnabled();
        revalidate();
    }

    private void updateAddEnabled() {
        addAction.setEnabled(!keyField.getText().isEmpty() && !translationField.getText().isEmpty() && !sortKeyField.getText().isEmpty() && !fields.isEmpty());
    }

    public boolean isApproved() {
        return approved;
    }

    public BatchAddInputs getInputs() {
        List<BatchAddInputs.Field> fields =
            this.fields.stream()
                .map(view -> new BatchAddInputs.Field(
                    view.nameField.getText(),
                    List.of(view.keysField.getText().strip().split(", +")),
                    List.of(view.translationsField.getText().strip().split(", +")),
                    view.groupCheckBox.isSelected()
                ))
                .toList();
        return new BatchAddInputs(fields, keyField.getText(), translationField.getText(), sortKeyField.getText());
    }

    private static final class FieldView extends JPanel {
        private final JCheckBox groupCheckBox = new JCheckBox("Group", false);
        private final JTextField nameField = new JTextField();
        private final JTextField keysField = new JTextField();
        private final JTextField translationsField = new JTextField();
        private final JButton deleteButton = new JButton("Delete");

        FieldView() {
            var layout = new GroupLayout(this);
            setLayout(layout);

            var nameLabel = new JLabel("Name");
            var keysLabel = new JLabel("Keys");
            var translationsLabel = new JLabel("Translations");

            layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addGroup(
                        layout.createParallelGroup()
                            .addComponent(nameLabel)
                            .addComponent(keysLabel)
                            .addComponent(translationsLabel)
                            .addComponent(groupCheckBox)
                    )
                    .addGap(5)
                    .addGroup(
                        layout.createParallelGroup()
                            .addComponent(nameField)
                            .addComponent(keysField)
                            .addComponent(translationsField)
                            .addComponent(deleteButton, GroupLayout.Alignment.TRAILING)
                    )
            );
            layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup().addComponent(nameLabel).addComponent(nameField))
                    .addGroup(layout.createParallelGroup().addComponent(keysLabel).addComponent(keysField))
                    .addGroup(layout.createParallelGroup().addComponent(translationsLabel).addComponent(translationsField))
                    .addGroup(layout.createParallelGroup().addComponent(groupCheckBox).addComponent(deleteButton))
            );
        }
    }
}
