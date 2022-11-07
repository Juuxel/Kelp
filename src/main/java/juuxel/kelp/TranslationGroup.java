/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.kelp;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public record TranslationGroup(@Nullable String pattern, @Nullable VariablePattern key, @Nullable VariablePattern defaultTranslation) {
    public boolean cannotGenerate() {
        return key == null || defaultTranslation == null;
    }

    public boolean containsKey(String key, Function<String, ? extends Collection<String>> variableQuery) {
        if (this.key == null) {
            return pattern == null || key.matches(pattern);
        }

        return this.key.matches(key, variableQuery) || (pattern != null && key.matches(pattern));
    }

    public Set<String> getVariables() {
        if (cannotGenerate()) return Set.of();

        Set<String> variables = new HashSet<>();
        variables.addAll(key.getVariables());
        variables.addAll(defaultTranslation.getVariables());
        return variables;
    }
}
