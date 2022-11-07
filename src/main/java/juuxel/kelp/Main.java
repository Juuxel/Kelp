package juuxel.kelp;

import com.google.common.collect.Sets;
import com.squareup.moshi.Moshi;
import okio.BufferedSource;
import okio.Okio;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

@Command(name = "kelp", mixinStandardHelpOptions = true)
public final class Main {
    private static final String FORMAT_FILE = "kelp_format.json";
    private static final String DEFAULT_LANGUAGE_FILE = "en_us.json";

    private final Moshi moshi = new Moshi.Builder()
        .add(new VariablePattern.Adapter())
        .add(new ContentKinds.Adapter())
        .add(new TranslationFile.Adapter())
        .build();

    private SortedMap<String, String> generate(FormatFile format, @Nullable TranslationFile currentTranslations) {
        TreeMap<String, String> translations = new TreeMap<>(format.getTranslationKeyComparator());
        if (currentTranslations != null) {
            translations.putAll(currentTranslations.translations());
        }

        for (TranslationGroup group : format.groups()) {
            if (group.cannotGenerate()) continue;

            List<Set<Triple<String>>> variables = CollectionUtil.map(
                group.getVariables(),
                var -> CollectionUtil.mapTo(
                    format.contentKinds().getAllValues(var).entrySet(),
                    entry -> new Triple<>(var, entry.getKey(), entry.getValue()),
                    new HashSet<>()
                )
            );
            Set<List<Triple<String>>> variableCombinations = Sets.cartesianProduct(variables);

            for (var combination : variableCombinations) {
                Map<String, String> keys = CollectionUtil.buildMap(combination, Triple::first, Triple::second);
                Map<String, String> values = CollectionUtil.buildMap(combination, Triple::first, Triple::third);
                assert group.key() != null && group.defaultTranslation() != null;
                String key = group.key().generate(keys);
                String value = group.defaultTranslation().generate(values);
                translations.put(key, value);
            }
        }

        return translations;
    }

    @Command(name = "generate")
    public void generate(
        @Parameters(index = "0", arity = "1") Path directory,
        @Parameters List<Path> adornDataXmls
    ) throws IOException {
        FormatFile format = fromJson(FormatFile.class, directory.resolve(FORMAT_FILE))
            .prependContentKinds(loadContentKindsFromAdornDataFiles(adornDataXmls));
        TranslationFile file = new TranslationFile(generate(format, null));
        writeTranslations(directory.resolve(DEFAULT_LANGUAGE_FILE), format, file);
    }

    @Command(name = "update")
    public void update(
        @Parameters(index = "0", arity = "1") Path directory,
        @Option(names = "--force") boolean force,
        @Parameters List<Path> adornDataXmls
    ) throws IOException {
        FormatFile format = fromJson(FormatFile.class, directory.resolve(FORMAT_FILE))
            .prependContentKinds(loadContentKindsFromAdornDataFiles(adornDataXmls));
        TranslationFile oldDefault = fromJson(TranslationFile.class, directory.resolve(DEFAULT_LANGUAGE_FILE));
        Map<String, String> defaultTranslations = generate(format, oldDefault);

        if (force || !oldDefault.translations().equals(defaultTranslations)) {
            TranslationFile newDefault = new TranslationFile(defaultTranslations);
            writeTranslations(directory.resolve(DEFAULT_LANGUAGE_FILE), format, newDefault);
        }

        List<Path> languageFiles = Files.list(directory)
            .filter(it -> {
                String name = it.getFileName().toString().toLowerCase(Locale.ROOT);
                return !name.equals(FORMAT_FILE) && !name.equals(DEFAULT_LANGUAGE_FILE) && name.endsWith(".json");
            }).toList();

        for (Path path : languageFiles) {
            TranslationFile file = fromJson(TranslationFile.class, path);
            Map<String, String> newTranslations = new TreeMap<>(format.getTranslationKeyComparator());
            newTranslations.putAll(file.translations());
            newTranslations.entrySet().removeIf(entry -> {
                boolean removing = !defaultTranslations.containsKey(entry.getKey());

                if (removing) {
                    System.err.printf("Removing '%s' from %s%n", entry.getKey(), path);
                }

                return removing;
            });

            if (force || !newTranslations.equals(file.translations())) {
                TranslationFile newFile = new TranslationFile(newTranslations);
                writeTranslations(path, format, newFile);
            }
        }
    }

    private static List<ContentKinds> loadContentKindsFromAdornDataFiles(List<Path> xmls) {
        return CollectionUtil.map(Objects.requireNonNullElse(xmls, List.of()), XmlLoader::load);
    }

    private <R> R fromJson(Class<R> clazz, Path path) throws IOException {
        try (BufferedSource source = Okio.buffer(Okio.source(path))) {
            return moshi.adapter(clazz).fromJson(source);
        }
    }

    private void writeTranslations(Path path, FormatFile format, TranslationFile translations) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            translations.toJson(format, writer);
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
