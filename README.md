<div align="center">
  <h1>block-replace</h1>
  <p><strong>Массовая замена блоков в мирах Minecraft Java Edition</strong></p>
  <p>Anvil (.mca) • McRegion (.mcr) • GUI + CLI в одном JAR</p>
  <br>
  <img src="screenshots/gui-main.png" alt="block-replace GUI" width="800">
  <br>
  <p>
    <a href="#-возможности">Возможности</a> •
    <a href="#-быстрый-старт">Быстрый старт</a> •
    <a href="#-документация">Документация</a> •
    <a href="#-скачать">Скачать</a>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-blue.svg">
    <img src="https://img.shields.io/badge/license-Unlicense-green.svg">
    <img src="https://img.shields.io/badge/Minecraft-1.21.1-blueviolet.svg">
  </p>
</div>

---

## 📸 Интерфейс

<div align="center">
  <img src="screenshots/Главное меню.png" alt="Главное меню" width="400">
  <img src="screenshots/Добавить задачу.png" alt="Добавить задачу" width="400">
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
