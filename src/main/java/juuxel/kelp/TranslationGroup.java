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
