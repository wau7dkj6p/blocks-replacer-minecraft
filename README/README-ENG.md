<div align="center">
  <h1>block-replace</h1>
  <p><strong>Mass block replacement tool for Minecraft Java Edition</strong></p>
  <p>Anvil (.mca) • McRegion (.mcr) • GUI + CLI in one JAR</p>
  <br>
  <img src="screenshots/gui-main.png" alt="block-replace GUI" width="800">
  <br>
  <p>
    <a href="#-features">Features</a> •
    <a href="#-quick-start">Quick start</a> •
    <a href="#-gui-overview">GUI</a> •
    <a href="#-cli-usage">CLI</a> •
    <a href="#-download">Download</a>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-blue.svg">
    <img src="https://img.shields.io/badge/license-Unlicense-green.svg">
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-blueviolet.svg">
  </p>
  <p>
    <a href="README.md">🏠 Main</a> •
    <a href="README-RU.md">🇷🇺 Русский</a>
  </p>
</div>

---

## 📸 Interface

<div align="center">
  <img src="screenshots/Главное меню.png" alt="Main window" width="400">
  <img src="screenshots/Добавить задачу.png" alt="Add task" width="400">
  <br>
  <em>Main window and task editor</em>
</div>

---

## ✨ Features

- **Two in one** — run without arguments (GUI) or with arguments (CLI)
- **Batch replacement** — multiple `FROM -> TO` rules in one pass
- **All dimensions** — Overworld, Nether, End
- **Safe by default** — region backups, state saving, resumable processing
- **Mods friendly** — `--allow-unknown-blocks` flag for custom blocks
- **Preview mode** — `--dry-run` without writing, `--scan-only` for block counting
- **Validation** — based on Minecraft 1.21.1 data (minecraft-data)

---

## 🚀 Quick start

### Option 1: GUI (simple)
```bash
java -jar block-replace-1.0.0-all.jar
```

### Option 2: CLI (for scripts)
```bash
# Replace snow with air
java -jar block-replace-1.0.0-all.jar \
  --level-dat "path/to/world/level.dat" \
  --from minecraft:snow \
  --to air

# Multiple tasks at once
java -jar block-replace-1.0.0-all.jar \
  --level-dat "path/to/world/level.dat" \
  --task "minecraft:snow->air" \
  --task "minecraft:snow_block->minecraft:stone"
```

### Option 3: Windows (no Java required)
Download `block-replace-windows-x64.zip`, extract and run `block-replace.exe`

---

## 🖥 GUI overview

When you launch the program **without arguments**, the GUI appears.

- **World selection**
  - Click **"Выбрать…"** and point to `level.dat` inside your world folder
  - The app shows the world name below the field

- **Dimensions**
  - Checkboxes: `Overworld` (enabled by default), `Nether`, `End`
  - At least one dimension must be selected

- **Task list**
  - Buttons: **Add task**, **Edit**, **Delete**, **Export**, **Import** (JSON)

- **Running tasks**
  - Bottom panel shows console log, progress bar and file counter
  - First run: **always creates backups** and saves state to `.block-replace-state.json`
  - If interrupted: you can resume later
  - When state file detected: GUI asks to continue or start fresh

---

## ⌨ CLI usage

### Basic commands

```bash
# Help
java -jar block-replace-1.0.0-all.jar --help

# Replace snow with air (Windows)
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "C:\Users\<user>\saves\MyWorld\level.dat" ^
  --from minecraft:snow ^
  --to air

# Replace snow with air (Linux/macOS)
java -jar block-replace-1.0.0-all.jar \
  --level-dat "/home/user/.minecraft/saves/MyWorld/level.dat" \
  --from minecraft:snow \
  --to air
```

### Multiple tasks

```bash
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --task "minecraft:snow->air" ^
  --task "minecraft:snow_block->minecraft:stone"
```

### Dimension selection

```bash
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "C:\path\to\world\level.dat" ^
  --dims overworld,nether ^
  --from minecraft:snow ^
  --to air
```

### Safety options

```bash
# Dry run (preview only)
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --dry-run

# With backups
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --backup
```

### Scan-only mode

```bash
# Count how many snow blocks in the world
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --scan-only ^
  --from minecraft:snow
```

### Long runs with resume

```bash
# First run with state saving
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --backup ^
  --save-state

# Resume later
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --backup ^
  --save-state ^
  --resume
```

### Modded worlds

```bash
# Allow unknown blocks (for mods)
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --from "mod:custom_block" ^
  --to air ^
  --allow-unknown-blocks

# Disable snowy ground fix
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --no-fix-snowy-ground
```

### Colored output

```bash
java -jar block-replace-1.0.0-all.jar ^
  --level-dat "path\to\level.dat" ^
  --from minecraft:snow ^
  --to air ^
  --ansi
```

---

## 📦 Download

### Current release: **v1.0.0**

| Version | Link | Requirements |
|--------|------|--------------|
| Fat JAR | [block-replace-1.0.0-all.jar](link_to_jar) | Java 21 |
| Windows | [block-replace-windows-x64.zip](link_to_zip) | Windows 10/11 (Java included) |

> 📝 **Note:** All releases available on [releases page](link_to_releases).

---

## 🛠 Building from source

```bash
# Clone
git clone https://github.com/your-username/block-replace.git
cd block-replace

# Build JAR
./gradlew :app:shadowJar
# JAR will be in app/build/libs/

# Build Windows version (requires jpackage)
./gradlew :app:jpackageImage
```

---

## ⚙️ Requirements

- **Java 21** (for JAR version)
- **Windows 10/11** (for EXE version)
- **Minecraft Java Edition** (any version, blocks validated against 1.21.1)

---

## 📄 License

The project is distributed under **The Unlicense** (public domain).  
See the [LICENSE](LICENSE) file for details.

---

## 🙏 Credits

- Block data from [minecraft-data](https://github.com/PrismarineJS/minecraft-data) project
- Minecraft is a trademark of Mojang Studios. This project is not affiliated with them.

---

<div align="center">
  <sub>Made with ❤️ for Minecraft community</sub>
  <br>
  <sub>Questions, bugs, ideas? → <a href="link_to_issues">Issues</a></sub>
  <br>
  <sub><a href="README.md">🏠 Back to main</a></sub>
</div>
