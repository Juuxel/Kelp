/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Args(boolean success, String[] args, Map<ArgsSchema.Entry, String> parsed) {
    public boolean hasFlag(ArgsSchema.Flag flag) {
        return parsed.containsKey(flag);
    }

    public boolean shouldShowHelp() {
        return !success || hasFlag(ArgsSchema.HELP);
    }

    public boolean shouldShowVersion() {
        return hasFlag(ArgsSchema.VERSION);
    }

    public int exitCode() {
        return success ? 0 : 1;
    }

    public record ArgsSchema(List<Positional> positionals, List<FlagLike> entries) {
        public static final Flag HELP = new Flag("-h", "--help");
        public static final Flag VERSION = new Flag("-V", "--version");

        public void showHelp() {
            System.out.print("Usage: translation-tool [options]");
            for (Positional positional : positionals) {
                System.out.printf(" [%s]", positional.name);
            }
            System.out.println();

            System.out.println("Options:");
            for (FlagLike entry : entries) {
                System.out.println("  " + String.join(", ", entry.names()));
            }
        }

        public sealed interface Entry {
        }

        public sealed interface FlagLike extends Entry {
            List<String> names();
        }

        public record Flag(List<String> names) implements FlagLike {
            public Flag(String... names) {
                this(List.of(names));
            }
        }

        public record Option(List<String> names) implements FlagLike {
        }

        public record Positional(String name) implements Entry {
        }
    }

    public static Args parse(String[] args, ArgsSchema schema) {
        Map<String, ArgsSchema.FlagLike> flagLikesByName = new HashMap<>();

        for (ArgsSchema.FlagLike entry : schema.entries) {
            for (String name : entry.names()) {
                flagLikesByName.put(name, entry);
            }
        }

        Map<ArgsSchema.Entry, String> parsed = new HashMap<>();
        int positionalIndex = 0;
        boolean acceptOptions = true;
        ArgsSchema.Option currentOption = null;
        boolean success = true;

        loop: for (var arg : args) {
            if (currentOption != null) {
                parsed.put(currentOption, arg);
                currentOption = null;
            } else if (acceptOptions && arg.equals("--")) {
                acceptOptions = false;
            } else if (acceptOptions && arg.startsWith("-")) {
                var entry = flagLikesByName.get(arg);
                switch (entry) {
                    case ArgsSchema.Flag _ -> parsed.put(entry, "");
                    case ArgsSchema.Option option -> currentOption = option;
                    case null -> {
                        success = false;
                        System.err.println("Unknown option or flag: " + arg);
                        break loop;
                    }
                }
            } else if (positionalIndex >= schema.positionals.size()) {
                System.err.println("Unexpected argument: " + arg);
                success = false;
                break;
            } else {
                parsed.put(schema.positionals.get(positionalIndex++), arg);
            }
        }

        if (currentOption != null) {
            System.err.println("Unterminated option: " + args[args.length - 1]);
            success = false;
        }

        return new Args(success, args, parsed);
    }
}
