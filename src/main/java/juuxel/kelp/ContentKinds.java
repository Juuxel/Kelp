package juuxel.kelp;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.util.LinkedHashMap;
import java.util.Map;

public record ContentKinds(Map<String, Map<String, String>> data) {
    public Map<String, String> getAllValues(String variable) {
        if (variable.indexOf('+') == -1) {
            return data.get(variable);
        }

        String[] variables = variable.split("\\+");
        Map<String, String> all = new LinkedHashMap<>();

        for (String var : variables) {
            all.putAll(data.get(var));
        }

        return all;
    }

    public static ContentKinds merge(Iterable<ContentKinds> kinds) {
        Map<String, Map<String, String>> topLevel = new LinkedHashMap<>();

        for (ContentKinds ck : kinds) {
            ck.data().forEach((variable, values) -> {
                Map<String, String> valueMap = topLevel.computeIfAbsent(variable, x -> new LinkedHashMap<>());
                valueMap.putAll(values);
            });
        }

        return new ContentKinds(topLevel);
    }

    public static final class Adapter extends JsonAdapter.ReadOnly<ContentKinds, Map<String, Map<String, String>>> {
        @Override
        @FromJson
        public ContentKinds fromJson(Map<String, Map<String, String>> data) {
            return new ContentKinds(data);
        }

        @Override
        @ToJson
        public Map<String, Map<String, String>> toJson(ContentKinds value) {
            return unsupported();
        }
    }
}
