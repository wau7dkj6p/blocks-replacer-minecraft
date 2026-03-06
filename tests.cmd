@echo off
setlocal

rem Перейти в каталог скрипта (корень проекта)
pushd "%~dp0" >nul 2>&1

echo Запуск тестов Gradle (gradlew.bat clean test -x :core:test)...
call gradlew.bat clean test -x :core:test

if errorlevel 1 (
  echo Ошибка: тесты завершились с ошибкой.
  popd
  endlocal
  exit /b %errorlevel%
)

echo Тесты успешно пройдены.

popd >nul 2>&1
endlocal
exit /b 0

