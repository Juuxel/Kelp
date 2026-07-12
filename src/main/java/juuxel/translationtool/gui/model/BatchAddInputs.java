/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record BatchAddInputs(List<Field> fields, String keyTemplate, String translationTemplate, String insertAfterKeyTemplate) {
    public BatchAddInputs {
        Objects.requireNonNull(fields);
        Objects.requireNonNull(keyTemplate);
        Objects.requireNonNull(translationTemplate);
        Objects.requireNonNull(insertAfterKeyTemplate);

        if (keyTemplate.isEmpty()) {
            throw new IllegalArgumentException("Key template cannot be empty");
        }
    }

    public record Field(String name, List<String> keys, List<String> translations, boolean group) {
        public Field {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("No name provided for template");
            }

            if (keys.size() != translations.size()) {
                throw new IllegalArgumentException("There must be equally many keys and translations");
            }

            if (keys.isEmpty()) {
                throw new IllegalArgumentException("Empty field " + name);
            }
        }

        public List<FieldInstance> instances() {
            List<FieldInstance> instances = new ArrayList<>(keys.size());

            for (int i = 0; i < keys.size(); i++) {
                instances.add(new FieldInstance(name, keys.get(i), translations.get(i)));
            }

            return instances;
        }
    }

    public record FieldInstance(String name, String key, String translation) {
    }
}
