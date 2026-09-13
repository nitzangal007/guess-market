@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "PROJECT_ROOT=%~dp0"
set "OUTPUT=%PROJECT_ROOT%build\javafx"
set "JAXB=%PROJECT_ROOT%tools\jaxb-ri-4.0.5\mod"
set "JUNIT=%PROJECT_ROOT%tools\testing\junit-platform-console-standalone-6.1.1.jar"
if "%JAVA_HOME%"=="" (
    echo ERROR: Set JAVA_HOME to Oracle JDK 25.0.4.
    exit /b 1
)
if "%JAVAFX_HOME%"=="" (
    echo ERROR: Set JAVAFX_HOME to the JavaFX 25.0.4 SDK directory.
    exit /b 1
)
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR=%JAVA_HOME%\bin\jar.exe"
set "FX=%JAVAFX_HOME%\lib"
for %%F in ("%JAVA%" "%JAVAC%" "%JAR%" "%JUNIT%" "%FX%\javafx.controls.jar" "%FX%\javafx.fxml.jar") do (
    if not exist "%%~fF" (
        echo ERROR: Missing input %%~fF
        exit /b 1
    )
)
"%JAVAC%" -version 2>&1 | findstr /x /c:"javac 25.0.4" >nul
if errorlevel 1 exit /b 1
powershell -NoProfile -Command "if((Get-Content -LiteralPath (Join-Path $env:FX 'javafx.properties')) -notcontains 'javafx.version=25.0.4'){exit 1}"
if errorlevel 1 (
    echo ERROR: JAVAFX_HOME must contain SDK 25.0.4.
    exit /b 1
)
set "JAXB_CP=%JAXB%\jakarta.activation-api.jar;%JAXB%\angus-activation.jar;%JAXB%\jakarta.xml.bind-api.jar;%JAXB%\jaxb-core.jar;%JAXB%\jaxb-impl.jar"
for %%F in (jakarta.activation-api.jar angus-activation.jar jakarta.xml.bind-api.jar jaxb-core.jar jaxb-impl.jar) do (
    if not exist "%JAXB%\%%F" exit /b 1
)
rem Clean only the verified development output, using one native shell.
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; $target=[IO.Path]::GetFullPath($env:OUTPUT); $expected=[IO.Path]::GetFullPath((Join-Path $env:PROJECT_ROOT 'build\javafx')); if($target -ne $expected -or $target -eq [IO.Path]::GetFullPath($env:PROJECT_ROOT)){throw 'Unsafe output path'}; if(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target -Recurse -Force}"
if errorlevel 1 exit /b 1
mkdir "%OUTPUT%\classes\dto" "%OUTPUT%\classes\engine" "%OUTPUT%\classes\ui" "%OUTPUT%\tests" "%OUTPUT%\reports" "%OUTPUT%\dev\lib" "%OUTPUT%\sources"
if errorlevel 1 exit /b 1
call :sources "%PROJECT_ROOT%modules\guessmarket-dto\src\main\java" "%OUTPUT%\sources\dto.txt"
if errorlevel 1 exit /b 1
call :sources "%PROJECT_ROOT%modules\guessmarket-engine\src\main\java" "%OUTPUT%\sources\engine.txt"
if errorlevel 1 exit /b 1
call :sources "%PROJECT_ROOT%modules\guessmarket-javafx-ui\src\main\java" "%OUTPUT%\sources\ui.txt"
if errorlevel 1 exit /b 1
call :tests
if errorlevel 1 exit /b 1
"%JAVAC%" --release 25 -encoding UTF-8 -Xlint:all -Werror -d "%OUTPUT%\classes\dto" @"%OUTPUT%\sources\dto.txt"
if errorlevel 1 exit /b 1
"%JAVAC%" --release 25 -encoding UTF-8 -Xlint:all -Werror -cp "%OUTPUT%\classes\dto;%JAXB_CP%" -d "%OUTPUT%\classes\engine" @"%OUTPUT%\sources\engine.txt"
if errorlevel 1 exit /b 1
xcopy /e /i /y "%PROJECT_ROOT%modules\guessmarket-engine\src\main\resources\*" "%OUTPUT%\classes\engine\" >nul
if errorlevel 1 exit /b 1
"%JAVAC%" --release 25 -encoding UTF-8 -Xlint:all -Werror --module-path "%FX%" --add-modules javafx.controls,javafx.fxml -cp "%OUTPUT%\classes\dto;%OUTPUT%\classes\engine" -d "%OUTPUT%\classes\ui" @"%OUTPUT%\sources\ui.txt"
if errorlevel 1 exit /b 1
xcopy /e /i /y "%PROJECT_ROOT%modules\guessmarket-javafx-ui\src\main\resources\*" "%OUTPUT%\classes\ui\" >nul
if errorlevel 1 exit /b 1
for %%F in (main.fxml main.css users.fxml) do if not exist "%OUTPUT%\classes\ui\guessmarket\ui\javafx\%%F" exit /b 1
"%JAR%" --create --file "%OUTPUT%\dev\lib\guessmarket-dto.jar" -C "%OUTPUT%\classes\dto" .
if errorlevel 1 exit /b 1
"%JAR%" --create --file "%OUTPUT%\dev\lib\guessmarket-engine.jar" -C "%OUTPUT%\classes\engine" .
if errorlevel 1 exit /b 1
"%JAR%" --create --file "%OUTPUT%\dev\lib\guessmarket-javafx-ui.jar" -C "%OUTPUT%\classes\ui" .
if errorlevel 1 exit /b 1
for %%F in (jakarta.activation-api.jar angus-activation.jar jakarta.xml.bind-api.jar jaxb-core.jar jaxb-impl.jar) do (
    copy /y "%JAXB%\%%F" "%OUTPUT%\dev\lib\%%F" >nul
    if errorlevel 1 exit /b 1
)
copy /y "%PROJECT_ROOT%packaging\run-javafx.bat" "%OUTPUT%\dev\run-javafx.bat" >nul
if errorlevel 1 exit /b 1
"%JAVAC%" --release 25 -encoding UTF-8 -Xlint:all -Werror --module-path "%FX%" --add-modules javafx.controls,javafx.fxml -cp "%OUTPUT%\dev\lib\*;%JUNIT%" -d "%OUTPUT%\tests" @"%OUTPUT%\sources\tests.txt"
if errorlevel 1 exit /b 1
xcopy /e /i /y "%PROJECT_ROOT%modules\guessmarket-engine\src\test\resources\*" "%OUTPUT%\tests\" >nul
if errorlevel 1 exit /b 1
mkdir "%OUTPUT%\test-work\modules\guessmarket-engine\src\test"
xcopy /e /i /y "%PROJECT_ROOT%modules\guessmarket-engine\src\test\resources\*" "%OUTPUT%\test-work\modules\guessmarket-engine\src\test\resources\" >nul
if errorlevel 1 exit /b 1
set "TEST_CP=%OUTPUT%\tests;%OUTPUT%\dev\lib\guessmarket-dto.jar;%OUTPUT%\dev\lib\guessmarket-engine.jar;%OUTPUT%\dev\lib\guessmarket-javafx-ui.jar;%JAXB_CP%"
pushd "%OUTPUT%\test-work"
"%JAVA%" -Dprism.order=sw "-Dguessmarket.uiEvidenceDirectory=%OUTPUT%\native" --module-path "%FX%" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -jar "%JUNIT%" execute --class-path "%TEST_CP%" --scan-classpath --fail-if-no-tests --disable-banner --details=summary --reports-dir "%OUTPUT%\reports" > "%OUTPUT%\reports\junit-output.txt" 2>&1
set "TEST_EXIT=%ERRORLEVEL%"
popd
type "%OUTPUT%\reports\junit-output.txt"
if not "%TEST_EXIT%"=="0" exit /b %TEST_EXIT%
powershell -NoProfile -Command "$x=[xml](Get-Content -Raw -LiteralPath (Join-Path $env:OUTPUT 'reports/TEST-junit-jupiter.xml')); if([int]$x.testsuite.tests -ne 236 -or [int]$x.testsuite.failures -ne 0 -or [int]$x.testsuite.errors -ne 0 -or [int]$x.testsuite.skipped -ne 0){exit 1}"
if errorlevel 1 exit /b 1
echo SUCCESS: JavaFX development JARs and all 236 tests passed.
echo Run: "%OUTPUT%\dev\run-javafx.bat"
exit /b 0

:sources
set "SOURCE_ROOT=%~1"
set "SOURCE_LIST=%~2"
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; $files=@(Get-ChildItem -LiteralPath $env:SOURCE_ROOT -Recurse -Filter '*.java'); if($files.Count -eq 0){throw 'No Java sources'}; $lines=$files | ForEach-Object {([char]34)+$_.FullName.Replace('\','/')+([char]34)}; [IO.File]::WriteAllLines($env:SOURCE_LIST,$lines,[Text.UTF8Encoding]::new($false))"
exit /b %ERRORLEVEL%

:tests
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; $roots=@('guessmarket-dto','guessmarket-engine','guessmarket-javafx-ui') | ForEach-Object {Join-Path $env:PROJECT_ROOT ('modules/'+$_+'/src/test/java')}; $files=@(Get-ChildItem -LiteralPath $roots -Recurse -Filter '*.java' -File | Sort-Object FullName); if($files.Count -eq 0){throw 'No test sources'}; $lines=$files | ForEach-Object {([char]34)+$_.FullName.Replace('\','/')+([char]34)}; [IO.File]::WriteAllLines((Join-Path $env:OUTPUT 'sources/tests.txt'),$lines,[Text.UTF8Encoding]::new($false))"
exit /b %ERRORLEVEL%
