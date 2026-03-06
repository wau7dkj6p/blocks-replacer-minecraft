## block-replace — примеры для консоли

Короткая шпаргалка по запуску `block-replace` из Windows-консоли (cmd / PowerShell) и через готовые скрипты.

---

### GUI из консоли

- **Через Gradle (из корня проекта)**:

  ```bat
  gradlew.bat :app:run
  ```

- **Через fat JAR (после `gradlew.bat :app:shadowJar`)**:

  ```bat
  java -jar app/build/libs/app-*-all.jar
  ```

- **Через собранный Windows-релиз**:

  ```bat
  cd release\exe
  block-replace.exe
  ```

---

### CLI: базовые команды (Windows)

- **Одна простая замена блоков**:

  ```bat
  java -jar app/build/libs/app-*-all.jar ^
    --level-dat "C:\Users\<user>\AppData\Roaming\.minecraft\saves\MyWorld\level.dat" ^
    --from minecraft:snow ^
    --to air
  ```

- **Несколько задач через `--task FROM->TO`**:

  ```bat
  java -jar app/build/libs/app-*-all.jar ^
    --level-dat "C:\path\to\world\level.dat" ^
    --task "minecraft:snow->air" ^
    --task "minecraft:snow_block->minecraft:stone"
  ```

- **Безопасный прогон (dry-run) с бэкапами**:

  ```bat
  java -jar app/build/libs/app-*-all.jar ^
    --level-dat "C:\path\to\world\level.dat" ^
    --from minecraft:snow ^
    --to air ^
    --backup ^
    --dry-run
  ```

- **Режим подсчёта блоков (`--scan-only`)**:

  ```bat
  java -jar app/build/libs/app-*-all.jar ^
    --level-dat "C:\path\to\world\level.dat" ^
    --scan-only ^
    --from minecraft:snow
  ```

---

### Скрипты для Windows

- `start.cmd` — запустить GUI через Gradle wrapper.
- `tests.cmd` — прогнать все тесты (`gradlew.bat clean test`).
- `deploy.cmd` — собрать релиз: fat JAR + Windows app-image под `release\`.

---

### Коротко для Unix-подобных систем

Из корня проекта:

```bash
./gradlew :app:run
./gradlew :app:shadowJar
java -jar app/build/libs/app-*-all.jar --help
```

Подробные описания всех опций CLI и GUI см. в `README.md` и в файле `RUNNING_ON_WINDOWS.md`.

