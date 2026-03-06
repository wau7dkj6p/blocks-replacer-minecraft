## block-replace

Mass block replacement tool for **Minecraft Java Edition** worlds in Anvil `.mca` and McRegion `.mcr`
formats. The project provides both a **JavaFX GUI** and a **picocli-based CLI** for batch replacing
blocks in existing worlds.

- **Safe by default**: creates backups of region files and can save/restore progress.
- **World formats**: supports standard Java world layouts with `region/` folders in overworld, nether and end.
- **Tasks**: replacements are described as `FROM -> TO` rules using normal block state syntax.

The tool relies on block definitions from **minecraft-data** (`blocks.json` for version **1.21.1**)
to validate block state strings.

---

### TL;DR

- **Build all + tests**: `gradlew clean build`
- **Run GUI (dev)**: `gradlew :app:run`
- **Run CLI help (dev)**: `gradlew :app:run --args="--help"`
- **Build fat JAR**: `gradlew :app:shadowJar` → `app/build/libs/app-*-all.jar`
- **Windows helpers**: `start.cmd` (GUI), `tests.cmd` (tests), `deploy.cmd` (release), `EXEMPLE.md` (console examples), `RUNNING_ON_WINDOWS.md` (details).

---

### Features

- **GUI and CLI in one binary**  
  One runnable JAR or Windows app-image:  
  - no arguments → GUI,  
  - any arguments → CLI (`block-replace --help` etc.).

- **Multiple replacement tasks**  
  Configure several `FROM -> TO` rules and run them in one pass.

- **Resumable processing**  
  Long-running jobs can save their state to `.block-replace-state.json` in the world root and resume later.

- **Dry run (CLI)**  
  Walks the world and reports how much would change without touching the files.

- **Scan-only mode (CLI)**  
  Counts how many times a certain block appears without building tasks.

- **Backup support**  
  For real runs, region files can be copied into sibling `*_bak` folders before rewriting.

---

### Supported worlds

- **Minecraft Java Edition** worlds using:
  - Anvil region format (`.mca`),
  - McRegion format (`.mcr`, Beta-era worlds).
- The tool expects a standard world layout:
  - Overworld: `world/region/`
  - Nether: `world/DIM-1/region/`
  - End: `world/DIM1/region/`
- Block names and properties are validated against **minecraft-data** for version **1.21.1**.  
  For modded worlds, or when using custom blocks, the CLI flag `--allow-unknown-blocks`
  (and GUI’s built‑in “mods‑friendly” behaviour) let you proceed even if some blocks are unknown.

---

## Quick start (EN)

### Requirements

- **Java 21** (JDK with `jpackage` if you want the Windows app-image).
- **Gradle wrapper** already included in the repo (`gradlew` / `gradlew.bat`).

### Build everything

```bash
gradlew clean build
```

This compiles all modules (`core`, `cli`, `gui`, `app`) and runs tests.

### Run from Gradle

#### GUI

```bash
gradlew :app:run
```

This starts the JavaFX GUI with a window called `block-replace`.

#### CLI

```bash
gradlew :app:run --args="--help"
```

All CLI options described below are available this way; arguments after `--args=` are passed
directly into the CLI entry point.

### Build a fat JAR

```bash
gradlew :app:shadowJar
```

The runnable “all-in-one” JAR will appear under:

- `app/build/libs/app-<version>-all.jar`

Run it as:

- **GUI:**

  ```bash
  java -jar app/build/libs/app-*-all.jar
  ```

- **CLI:**

  ```bash
  java -jar app/build/libs/app-*-all.jar --help
  ```

### Windows app-image via jpackage

If your JDK includes `jpackage`, you can build a double-clickable Windows app-image:

```bash
gradlew :app:jpackageImage
```

The resulting image (with its own embedded runtime and `block-replace.exe`) will be created under:

- `app/build/jpackage/block-replace/`

On Windows you can run `block-replace.exe` from there or move the entire folder anywhere else.

---

## GUI overview (JavaFX)

When you launch the program **without arguments**, the GUI appears.

- **World selection**
  - Click **“Выбрать…”** and point to `level.dat` inside your world folder.
  - The application shows the world name (folder name) below the field.

- **Dimensions**
  - Checkboxes:
    - `Overworld` (enabled by default),
    - `Nether`,
    - `End`.
  - At least one dimension must be selected before running.

- **Task list**
  - Tasks are shown in a list with either their title or `FROM -> TO` string.
  - Buttons:
    - **“Добавить задачу”** – open the editor for a new task.
    - **“Редактировать выбранное”** – edit the selected task.
    - **“Удалить выбранное”** – delete selected tasks.
    - **“Экспортировать”** – save tasks as a JSON file.
    - **“Импортировать”** – load tasks from a JSON file.

- **Running tasks**
  - Bottom panel shows:
    - Text console with progress messages.
    - **“Выполнить задачи”** button.
    - Progress bar and `X / Y файлов` counter for region files.
  - On first run:
    - The tool **always creates backups** of region files and **saves state** in
      `.block-replace-state.json` in the world root.
  - If you stop the run or close the program:
    - The state file is kept so you can resume later.
  - When a state file is detected for the chosen world:
    - The GUI asks whether to **continue** from the saved state or **start a new session**.
    - After a fully successful run, the state file is deleted and the button text returns
      to “Выполнить задачи”.

Internally the GUI uses the same `WorldProcessor` as the CLI, but runs it on a background executor
so the JavaFX UI remains responsive. All updates to the window are marshalled onto the JavaFX
Application Thread.

---

## CLI usage

The CLI is implemented in `RootCommand` and is available either through Gradle
(`gradlew :app:run --args="..."`) or the fat JAR:

```bash
java -jar app/build/libs/app-*-all.jar --help
```

### Basic example

Replace all `minecraft:snow` with `air` in the overworld of a world located at
`C:\Users\<user>\AppData\Roaming\.minecraft\saves\MyWorld`:

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\Users\<user>\AppData\Roaming\.minecraft\saves\MyWorld\level.dat" ^
  --from minecraft:snow ^
  --to air
```

On Unix-like systems the same call looks like:

```bash
java -jar app/build/libs/app-*-all.jar \
  --level-dat "/home/<user>/.minecraft/saves/MyWorld/level.dat" \
  --from minecraft:snow \
  --to air
```

### Multi-task example (`--task FROM->TO`)

You can add several replacements at once using repeated `--task` options:

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --task "minecraft:snow->air" ^
  --task "minecraft:snow_block->minecraft:stone"
```

Each `FROM` / `TO` part uses the same block state syntax as the GUI:

- `minecraft:block_name`
- `minecraft:block_name[property1=value1,property2=value2]`

### Dimension selection (`--dims`)

By default only the overworld is processed. To target several dimensions:

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --dims overworld,nether ^
  --from minecraft:snow ^
  --to air
```

Valid values: `overworld`, `nether`, `end` (comma-separated, case-insensitive).

### Safety options: `--dry-run` and `--backup`

Perform a dry run that prints what would change without touching files:

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --dry-run
```

For real runs it is recommended to enable backups (CLI default is “no backup” unless you specify it):

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --backup
```

Backups are created next to the original region folders as `region_bak`, `DIM-1_bak`, etc.

### Scan-only mode (`--scan-only`)

Count how many times a block appears in the world without preparing replacement tasks:

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --scan-only ^
  --from minecraft:snow
```

Here `--from` is used only for the block name; properties are ignored.

### Long runs: `--save-state` and `--resume`

For large worlds you can save and resume processing from the CLI as well:

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --backup ^
  --save-state
```

This creates `.block-replace-state.json` in the world root and updates it after each processed
region. To continue later:

```bash
java -jar app/build/libs/app-*-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --backup ^
  --save-state ^
  --resume
```

Resuming is possible only when:

- world root is the same,
- task list is the same and in the same order,
- `--allow-unknown-blocks` and `--no-fix-snowy-ground` flags match the old run.

If configuration does not match, the state file is ignored and processing starts from scratch.

### Modded worlds and snowy ground

- `--allow-unknown-blocks` – do not fail if block names or properties are not known
  (useful for heavily modded worlds).
- `--no-fix-snowy-ground` – disables extra logic that fixes `snowy=true` grass/podzol/mycelium
  when snow above is removed.

### Colored console output

Add `--ansi` to enable ANSI colors for warnings and errors in terminals that support them.

---

## Dependencies

Core libraries used by the project:

- **JavaFX 21.0.7** – GUI and application window (via `org.openjfx.javafxplugin`).
- **picocli 4.7.7** – command line parser and help for the CLI.
- **Jackson Databind 2.21.1** – JSON handling for block database and saved state files, and for
  importing/exporting tasks in the GUI.
- **lz4-java 1.8.1** – LZ4 compression support for region data.
- **JUnit 5** – tests for core logic.
- **minecraft-data JSON** – `blocks.json` for `pc-1.21.1` (see Credits below).

---

## License

The project is distributed under **The Unlicense** (public domain dedication).  
See the [`LICENSE`](LICENSE) file for the full text.

This gives you as much freedom as possible to copy, modify, distribute and use the code for any
purpose, including commercial, with no conditions, as far as applicable law allows.  
Third-party data such as `minecraft-data` is covered by its own license (see below).

---

## Credits and third‑party data

- **minecraft-data**  
  Block definitions are based on `blocks.json` for `pc-1.21.1` from the
  [`minecraft-data` project](https://github.com/PrismarineJS/minecraft-data).  
  The original data and its license apply to that JSON file; this repository only bundles a copy
  for validation purposes.

Minecraft is a trademark of Mojang Studios. This project is not affiliated with or endorsed by
Mojang or Microsoft.

---
