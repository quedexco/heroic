# Hacking with IDEA

Import the project as a Maven project.

Make sure to install the [Lombok plugin](https://plugins.jetbrains.com/plugin/6317)
(File &rarr; Settings &rarr; Plugins &rarr; Browse Repositories...).

There is a [code style available](code-style.xml).
Copy the file into `<idea-home>/config/codestyles/Heroic.xml`, where
`<idea-home>` is the home directory of IDEA (`~/.IdeaIC15`, `~/.IntelliJIdea15/`).
After starting IDEA you can select the code style under
(File &rarr; Settings &rarr; Editor &rarr; Code Style) and pick scheme `Heroic`.

IDEA requires you to run `mvn compile` prior to the first run in order to
generate certain sources.

* `heroic-parser` - Contains Antlr4 grammar files which are not generated
    automatically by IDEA.

## Running HeroicShell in IDEA

Create a new `Application` run configuration where you use the following
parameters:

| Option                       | Value                                  |
|------------------------------|----------------------------------------|
| **Name**:                    | `HeroicShell` (or per your preference) |
| **Main class:**              | `com.spotify.heroic.HeroicShell`       |
| **Program arguments:**       | `-P memory --server`                   |
| **Use classpath of module:** | `heroic-dist`                          |

The above configuration sets up an in-memory configuration of Heroic, running
it will cause an interactive prompt to become available in the `Run` window.

You can see the list of available arguments by running `tools/heroic-shell
--help`.
