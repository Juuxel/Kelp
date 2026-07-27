# Kelp

A tool for managing Minecraft mod translation files.

## Usage

Run the jar as a module (`io.github.juuxel.translationtool`).
If given a file path as a command line argument, it will open that `lang` directory
or individual translation file.

Kelp can also be operated from the command line for linting
language files.

### `--lint` subcommand

Usage: `java -p kelp.jar -m io.github.juuxel.translationtool --lint <path> [-v | --verbose]`

Checks that the format in the given individual language file or `lang` directory
matches the expected format.

### `--reformat` subcommand

Usage: `java -p kelp.jar -m io.github.juuxel.translationtool --reformat <path> [--dry-run] [-v | --verbose]`

Reformats the given individual language file or `lang` directory
in the expected format.

With the `--dry-run` flag, only logs changes flagged by `--verbose`
(i.e. removed translation entries).
