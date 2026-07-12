/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui.model;

import juuxel.translationtool.schema.Schema;

import java.util.List;

public sealed interface TranslationModel permits Project, TranslationFile {
    List<TranslationFile> getFiles();

    default TranslationFile getPrimaryFile() {
        return getFiles().getFirst();
    }

    Schema getSchema();
    String getDescription();
    void renameKey(TranslationFile origin, String from, String to);
    void deleteRows(TranslationFile origin, List<String> keys);
}
