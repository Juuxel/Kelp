# Kelp

A tool for managing Minecraft mod translation files.

## Usage

Run the jar as a module. If given a file path, it will open that `lang` directory
or individual translation file.

If additionally given the `--reformat` flag, Kelp will reformat the translation file(s)
using its formatting rules. For directories, all files will be formatted using
the same layout as `en_us`, or the first language alphabetically if US English is not found.
