@echo off
setlocal

rem Change to the directory of this script (project root)
pushd "%~dp0" >nul 2>&1

echo [1/4] Building fat-jar with :app:shadowJar...
call gradlew.bat :app:shadowJar
if errorlevel 1 (
  echo Error: Gradle task :app:shadowJar failed.
  popd
  endlocal
  exit /b %errorlevel%
)

echo [2/4] Building Windows app-image with :app:prepareRelease...
call gradlew.bat :app:prepareRelease
if errorlevel 1 (
  echo Error: Gradle task :app:prepareRelease failed.
  popd
  endlocal
  exit /b %errorlevel%
)

echo [3/4] Preparing release folders...
if not exist "release" (
  mkdir "release"
  if errorlevel 1 (
    echo Error: failed to create directory release.
    popd
    endlocal
    exit /b %errorlevel%
  )
)

rem Check that the app-image was built into release\block-replace
if not exist "release\block-replace" (
  echo Error: expected folder release\block-replace was not found.
  echo The task :app:prepareRelease should have created it.
  popd
  endlocal
  exit /b 1
)

echo [4/4] Copying artifacts...

set "JAR_FILE="
for %%F in ("app\build\libs\*-all.jar") do (
  set "JAR_FILE=%%F"
)

if not defined JAR_FILE (
  echo Error: fat-jar not found: app\build\libs\*-all.jar
  popd
  endlocal
  exit /b 1
)

rem Copy the fat‑jar directly into release\block-replace
copy /Y "%JAR_FILE%" "release\block-replace\block-replace.jar" >nul
if errorlevel 1 (
  echo Error: failed to copy jar to release\block-replace\block-replace.jar.
  popd
  endlocal
  exit /b %errorlevel%
)

rem Ensure block-replace.cfg exists in release\block-replace\app (launcher requires it)
if exist "release\block-replace\app\block-replace.cfg" (
  rem Already present – nothing to do
) else (
  echo [Application]> "release\block-replace\app\block-replace.cfg"
  echo app.classpath=$APPDIR\app-0.1.0-SNAPSHOT-all.jar>> "release\block-replace\app\block-replace.cfg"
  echo app.mainclass=com.blockreplace.app.Main>> "release\block-replace\app\block-replace.cfg"
  echo app.classpath=$APPDIR\app-0.1.0-SNAPSHOT.jar>> "release\block-replace\app\block-replace.cfg"
  echo.>> "release\block-replace\app\block-replace.cfg"
  echo [JavaOptions]>> "release\block-replace\app\block-replace.cfg"
  echo java-options=-Djpackage.app-version=0.1.0>> "release\block-replace\app\block-replace.cfg"
)

rem Copy start-debug.cmd so user can run app with visible errors when exe fails
if exist "start-debug.cmd" (
  copy /Y "start-debug.cmd" "release\block-replace\start-debug.cmd" >nul
)

echo.
echo Done.
echo   - release\block-replace\block-replace.jar
echo   - release\block-replace\block-replace.exe (inside the app-image folder)

popd >nul 2>&1
endlocal
exit /b 0