@echo off
setlocal

rem Перейти в каталог скрипта (корень проекта)
pushd "%~dp0" >nul 2>&1

echo Запуск GUI block-replace через Gradle wrapper...
echo (для CLI используйте: gradlew.bat :app:run --args="--help")

call gradlew.bat :app:run --no-daemon %*
if errorlevel 1 (
  echo Ошибка: Gradle-задача :app:run завершилась с ошибкой.
  popd
  endlocal
  exit /b %errorlevel%
)

popd >nul 2>&1
endlocal
exit /b 0
