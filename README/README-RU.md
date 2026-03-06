<div align="center">
  <h1>block-replace</h1>
  <p><strong>Массовая замена блоков в мирах Minecraft Java Edition</strong></p>
  <p>Anvil (.mca) • McRegion (.mcr) • GUI + CLI в одном JAR</p>
  <br>
  <img src="screenshots/gui-main.png" alt="block-replace GUI" width="800">
  <br>
  <p>
    <a href="#-что-это">Что это</a> •
    <a href="#-возможности">Возможности</a> •
    <a href="#-поддерживаемые-миры">Миры</a> •
    <a href="#-сборка-и-запуск">Сборка</a> •
    <a href="#-как-работать">Как работать</a>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-blue.svg">
    <img src="https://img.shields.io/badge/license-Unlicense-green.svg">
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-blueviolet.svg">
  </p>
  <p>
    <a href="README.md">🏠 Главная</a> •
    <a href="README-ENG.md">🇬🇧 English</a>
  </p>
</div>

---

## 📸 Интерфейс

<div align="center">
  <img src="screenshots/Главное меню.png" alt="Главное меню" width="400">
  <img src="screenshots/Добавить задачу.png" alt="Добавить задачу" width="400">
  <br>
  <em>Главное окно и редактор задач</em>
</div>

---

## 🔧 Что это за инструмент

`block-replace` — утилита для пакетной замены блоков в мирах **Minecraft Java Edition**
в форматах Anvil (`.mca`) и McRegion (`.mcr`).  
Приложение умеет работать как через **графический интерфейс (JavaFX)**, так и через
**консольную утилиту** (CLI).

Основная идея: вы описываете один или несколько правил вида `ИЗ -> В`, после чего программа
проходит по регионам и чанкам мира и применяет эти замены.

---

## ✨ Возможности

- **Два в одном** — запусти без аргументов (GUI) или с аргументами (CLI)
- **Массовая замена** — несколько правил `FROM -> TO` за один проход
- **Все измерения** — Overworld, Nether, End
- **Безопасно** — резервные копии регионов (`*_bak`), сохранение состояния, возобновление
- **Для модов** — флаг `--allow-unknown-blocks` для кастомных блоков
- **Проверка** — `--dry-run` без записи, `--scan-only` для подсчёта блоков
- **Валидация** — по данным Minecraft 1.21.1 (minecraft-data)

---

## 🌍 Поддерживаемые миры

- Обычные миры Minecraft Java с папкой `region/`:
  - `world/region/` — Overworld
  - `world/DIM-1/region/` — Nether
  - `world/DIM1/region/` — End
- Для проверки блоков используется база `blocks.json` из проекта `minecraft-data` для версии `1.21.1`
- Для модовых миров можно включить режим терпимого отношения к неизвестным блокам:
  - в CLI — флаг `--allow-unknown-blocks`
  - в GUI этот режим включён по умолчанию

---

## 🛠 Сборка и запуск

### Требования
- **Java 21**
- Gradle (в репозитории уже есть `gradlew`)

### Команды

```bash
# Базовая сборка
gradlew clean build

# Запуск GUI напрямую из Gradle
gradlew :app:run

# Сборка fat JAR
gradlew :app:shadowJar
```

После сборки JAR можно запускать:

```bash
java -jar app/build/libs/app-*-all.jar
```

---

## 🖥 Как работать через GUI

1. Выберите `level.dat` своего мира (кнопка **“Выбрать…”**)
2. Отметьте измерения (Overworld / Nether / End), которые нужно обработать
3. Добавьте одну или несколько задач замены:
   - через кнопку **“Добавить задачу”**
   - либо импортировав JSON с задачами
4. Нажмите **“Выполнить задачи”**:
   - появится лог работы и индикатор прогресса по регион-файлам
   - при первом запуске создаются резервные копии
5. Если выполнение было остановлено:
   - рядом с миром останется файл состояния `.block-replace-state.json`
   - при следующем запуске GUI спросит, продолжить ли с места остановки

---

## ⌨ Как работать через CLI

У CLI и GUI одна и та же «начинка», но параметры задаются флагами:

```bash
java -jar block-replace-1.0.0-all.jar [опции]
```

### Основные флаги

| Флаг | Описание |
|------|----------|
| `--level-dat` | Путь к `level.dat` внутри папки мира |
| `--dims` | Список измерений: `overworld,nether,end` |
| `--from` / `--to` | Одна задача замены |
| `--task FROM->TO` | Несколько задач (можно повторять) |
| `--dry-run` | Только подсчёт изменений без записи |
| `--backup` | Создание резервных копий регионов |
| `--scan-only` | Подсчёт количества заданного блока |
| `--save-state` / `--resume` | Сохранить и продолжить обработку |
| `--allow-unknown-blocks` | Разрешить неизвестные блоки (моды) |
| `--no-fix-snowy-ground` | Отключить исправление `snowy=true` |

### Примеры

```bash
# Замена снега на воздух
java -jar block-replace-1.0.0-all.jar \
  --level-dat "C:\Users\Имя\saves\Новый мир\level.dat" \
  --from minecraft:snow \
  --to air

# Несколько задач
java -jar block-replace-1.0.0-all.jar \
  --level-dat "путь\к\level.dat" \
  --task "minecraft:snow->air" \
  --task "minecraft:dirt->minecraft:grass_block"

# Сухой прогон с резервным копированием
java -jar block-replace-1.0.0-all.jar \
  --level-dat "путь\к\level.dat" \
  --from minecraft:snow \
  --to air \
  --dry-run \
  --backup
```

---

## 📄 Лицензия

Проект распространяется под лицензию **The Unlicense** — общественное достояние.  
Подробнее в файле [LICENSE](LICENSE).

---

## 🙏 Благодарности

- Данные о блоках: [minecraft-data](https://github.com/PrismarineJS/minecraft-data)
- Minecraft — торговая марка Mojang Studios. Проект не связан с ними.

---

<div align="center">
  <sub>Вопросы, баги, идеи? → <a href="ссылка_на_issues">Issues</a></sub>
  <br>
  <sub><a href="README.md">🏠 На главную</a></sub>
</div>
