package juuxel.kelp;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class VariablePattern {
    private static final String VARIABLE_REGEX = "([a-z0-9._]+)";
    private static final char VARIABLE_TOGGLE = '|';
    private final List<Component> components;

    private VariablePattern(List<Component> components) {
        this.components = components;
    }

    public static VariablePattern parse(String pattern) {
        List<Component> components = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean inVariable = false;

        for (char c : pattern.toCharArray()) {
            if (c == VARIABLE_TOGGLE) {
                components.add(createComponent(inVariable, buffer.toString()));
                buffer.setLength(0);
                inVariable = !inVariable;
            } else {
                buffer.append(c);
            }
        }

        if (!buffer.isEmpty()) {
            components.add(createComponent(inVariable, buffer.toString()));
        }

        return new VariablePattern(components);
    }

    private static Component createComponent(boolean variable, String content) {
        return variable ? new Component.OfVariable(content) : new Component.OfString(content);
    }

    private Pattern getPattern(Function<String, ? extends Collection<String>> variableQuery) {
        StringBuilder regex = new StringBuilder("^");

        for (var component : components) {
            regex.append(
                component.<String>fold(
                    c -> Pattern.quote(c.value()),
                    var -> '(' + String.join("|", variableQuery.apply(var.name())) + ')'
                )
            );
        }

        return Pattern.compile(regex.toString());
    }

    public boolean matches(String key, Function<String, ? extends Collection<String>> variableQuery) {
        return getPattern(variableQuery).matcher(key).matches();
    }

    public @Nullable Map<String, String> getVariablesFrom(String key, Function<String, ? extends Collection<String>> variableQuery) {
        Matcher matcher = getPattern(variableQuery).matcher(key);

        if (matcher.matches()) {
            List<String> variableNames = getOrderedVariables();
            Map<String, String> values = new LinkedHashMap<>();

            for (int i = 0; i < variableNames.size(); i++) {
                values.putIfAbsent(variableNames.get(i), matcher.group(i + 1));
            }

            return values;
        } else {
            return null;
        }
    }

    public String generate(Map<String, String> values) {
        return components.stream()
            .map(comp -> comp.fold(Component.OfString::value, var -> values.get(var.name())))
            .collect(Collectors.joining());
    }

    private List<String> getOrderedVariables() {
        return components.stream().filter(x -> x instanceof Component.OfVariable)
            .map(x -> ((Component.OfVariable) x).name())
            .toList();
    }

    public Set<String> getVariables() {
        return Set.copyOf(getOrderedVariables());
    }

    @Override
    public String toString() {
        return "VariablePattern[" + components.stream()
            .map(comp -> comp.fold(Component.OfString::value, var -> VARIABLE_TOGGLE + var.name() + VARIABLE_TOGGLE))
            .collect(Collectors.joining()) + "]";
    }

    public static final class Adapter extends JsonAdapter.ReadOnly<VariablePattern, String> {
        @Override
        @FromJson
        public VariablePattern fromJson(String pattern) {
            return parse(pattern);
        }

        @Override
        @ToJson
        public String toJson(VariablePattern value) {
            return unsupported();
        }
    }

    private sealed interface Component {
        <R> R fold(Function<OfString, R> ofString, Function<OfVariable, R> ofVariable);

        record OfString(String value) implements Component {
            @Override
            public <R> R fold(Function<OfString, R> ofString, Function<OfVariable, R> ofVariable) {
                return ofString.apply(this);
            }
        }

        record OfVariable(String name) implements Component {
            @Override
            public <R> R fold(Function<OfString, R> ofString, Function<OfVariable, R> ofVariable) {
                return ofVariable.apply(this);
            }
        }
    }
}
