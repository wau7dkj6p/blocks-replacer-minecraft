<div align="center">
  <h1><strong>Массовая замена блоков в мирах<br>Minecraft Java Edition</strong></h1>
  <p>Anvil (.mca) • McRegion (.mcr) • GUI + CLI в одном JAR</p>
  <img src="screenshots/Главное меню.png" alt="block-replace GUI" width="800">
  <p>
    <a href="#-возможности">Возможности</a> •
    <a href="#-быстрый-старт">Быстрый старт</a> •
    <a href="#-документация">Документация</a> •
    <a href="#-скачать">Скачать</a>
  </p>
  <p>
    <a href="README/README-RU.md">Русский</a> • <img src="https://img.shields.io/badge/Java-21-blue.svg"> 
    <img src="https://img.shields.io/badge/license-Unlicense-green.svg">
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-blueviolet.svg"> • <a href="README/README-ENG.md">English</a>
  </p>
  <p>
    
    
  </p>
</div>

---

<h2 align="center">📸 Редактор задач</h2>

<div align="center">
  <img src="screenshots/Добавить задачу.png" alt="Добавить задачу" width="600">
  <br>
</div>

---

## 🔥 Возможности

- **Два в одном** — запусти без аргументов (GUI) или с аргументами (CLI)
- **Массовая замена** — несколько правил `FROM -> TO` за один проход
- **Все измерения** — Overworld, Nether, End
- **Безопасно** — резервные копии регионов, сохранение состояния, возобновление после остановки
- **Для модов** — флаг `--allow-unknown-blocks` для кастомных блоков
- **Проверка** — `--dry-run` без записи, `--scan-only` для подсчёта блоков
- **Валидация** — по данным Minecraft 1.21.1 (minecraft-data)

---

## 🚀 Быстрый старт

### Вариант 1: GUI (просто)
```bash
java -jar block-replace-1.0.0-all.jar
```

### Вариант 2: CLI (для скриптов)
```bash
# Замена снега на воздух
java -jar block-replace-1.0.0-all.jar \
  --level-dat "путь/к/world/level.dat" \
  --from minecraft:snow \
  --to air

# Несколько задач сразу
java -jar block-replace-1.0.0-all.jar \
  --level-dat "путь/к/world/level.dat" \
  --task "minecraft:snow->air" \
  --task "minecraft:snow_block->minecraft:stone"
```

### Вариант 3: Windows (без Java)
Скачай `block-replace-windows-x64.zip`, распакуй и запусти `block-replace.exe`

---

## 📚 Документация

Проект имеет подробную документацию на двух языках:

| | |
|---|---|
| <img src="https://flagcdn.com/ru.svg" width="20"> [**Русская версия**](README/README-RU.md) | Полное описание на русском языке |
| <img src="https://flagcdn.com/gb.svg" width="20"> [**English version**](README/README-ENG.md) | Full documentation in English |

В документации:
- Установка и сборка
- Работа с GUI (по шагам)
- Все команды CLI с примерами
- Формат задач и блоков
- Лицензия и благодарности

---

## 📦 Скачать

### Текущий релиз: **v1.0.0**

| Версия | Ссылка | Требования |
|--------|--------|------------|
| Fat JAR | [block-replace-1.0.0-all.jar](https://github.com/wau7dkj6p/blocks-replacer-minecraft/releases/download/v1.0.0/block-replace-1.0.0-all.jar) | Java 21 |
| Windows | [block-replace-windows-x64.zip](https://github.com/wau7dkj6p/blocks-replacer-minecraft/releases/download/v1.0.0/block-replace-windows-x64.zip) | Windows 10/11 (Java внутри) |

> 📝 **Примечание:** Все релизы доступны на [странице релизов](https://github.com/wau7dkj6p/blocks-replacer-minecraft/releases).

---

## 🛠 Сборка из исходников

```bash
# Клонируем
git clone https://github.com/твой-username/block-replace.git
cd block-replace

# Собираем JAR
./gradlew :app:shadowJar
# JAR будет в app/build/libs/

# Собираем Windows версию (нужен jpackage)
./gradlew :app:jpackageImage
```

---

## ⚙️ Требования

- **Java 21** (для JAR-версии)
- **Windows 10/11** (для exe-версии)
- **Minecraft Java Edition** (любая версия, но блоки валидируются по 1.21.1)

---

## 📄 Лицензия

Проект распространяется под лицензию **The Unlicense** — это означает, что код находится в общественном достоянии. Можете использовать, модифицировать и распространять без каких-либо ограничений.

Подробнее: [LICENSE](LICENSE)

---

## 🙏 Благодарности

- Данные о блоках взяты из проекта [minecraft-data](https://github.com/PrismarineJS/minecraft-data)
- Minecraft является торговой маркой Mojang Studios. Проект не связан с Mojang или Microsoft.

---

<div align="center">
  <sub>Сделано с ❤️ для сообщества Minecraft</sub>
  <br>
  <sub>Вопросы, баги, идеи? → <a href="https://github.com/wau7dkj6p/blocks-replacer-minecraft/issues">Issues</a></sub>
</div>

