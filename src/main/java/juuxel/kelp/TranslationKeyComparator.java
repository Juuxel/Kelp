package juuxel.kelp;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TranslationKeyComparator implements Comparator<String> {
    private final FormatFile format;
    private final Cache<String, List<String>> variableValueCache;
    private final Cache<String, Integer> groupIndexCache;
    private final Cache<String, VariableKey> variableKeyCache;

    TranslationKeyComparator(FormatFile format) {
        this.format = format;
        this.variableValueCache = new Cache<>(x -> List.copyOf(format.contentKinds().getAllValues(x).keySet()));
        this.groupIndexCache = new Cache<>(x -> CollectionUtil.indexOf(format.groups(), group -> group.containsKey(x, variableValueCache::get)));
        this.variableKeyCache = new Cache<>(this::createVariableKey);
    }

    public TranslationGroup getGroup(String translationKey) {
        return format.groups().get(groupIndexCache.get(translationKey));
    }

    @Override
    public int compare(String a, String b) {
        if (Objects.equals(a, b)) return 0;

        int aGroupIndex = groupIndexCache.get(a);
        int bGroupIndex = groupIndexCache.get(b);
        int result = Integer.compare(aGroupIndex, bGroupIndex);

        if (result == 0) {
            VariableKey aKey = variableKeyCache.get(a);
            VariableKey bKey = variableKeyCache.get(b);

            if (aKey.tailing) {
                return bKey.tailing
                    ? a.compareTo(b)
                    : 1; // a > b
            } else if (bKey.tailing) {
                return -1; // a < b
            }

            int aCount = aKey.values.size();
            int bCount = bKey.values.size();
            int max = Math.max(aCount, bCount);

            for (int i = 0; i < max; i++) {
                if (i >= aCount) {
                    return -1; // a < b
                } else if (i >= bCount) {
                    return 1; // a > b
                }

                String aVar = aKey.variables.get(i);
                String bVar = bKey.variables.get(i);

                if (aVar.equals(bVar)) {
                    String aVal = aKey.values.get(i);
                    String bVal = bKey.values.get(i);

                    if (!aVal.equals(bVal)) {
                        List<String> variableValues = variableValueCache.get(aVar);
                        int aVarIndex = variableValues.indexOf(aVal);
                        int bVarIndex = variableValues.indexOf(bVal);

                        if (aVarIndex != bVarIndex) {
                            return Integer.compare(aVarIndex, bVarIndex);
                        }
                    }
                }
            }

            return a.compareTo(b);
        }

        return result;
    }

    private VariableKey createVariableKey(String translationKey) {
        TranslationGroup group = format.groups().get(groupIndexCache.get(translationKey));

        if (group.key() == null) {
            return new VariableKey(null, List.of(), group.pattern() != null);
        }

        Map<String, String> variables = group.key().getVariablesFrom(translationKey, variableValueCache::get);

        if (variables != null) {
            return new VariableKey(List.copyOf(variables.keySet()), List.copyOf(variables.values()), false);
        } else {
            return new VariableKey(null, List.of(), group.pattern() != null);
        }
    }

    private record VariableKey(List<String> variables, List<String> values, boolean tailing) {
    }
}
