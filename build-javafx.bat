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
call :sources "%PROJECT_ROOT%modules\guessmarket-javafx-ui\src\test\java" "%OUTPUT%\sources\tests.txt"
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
"%JAVAC%" --release 25 -encoding UTF-8 -Xlint:all -Werror -cp "%OUTPUT%\dev\lib\*;%JUNIT%" -d "%OUTPUT%\tests" "%PROJECT_ROOT%modules\guessmarket-engine\src\test\java\guessmarket\engine\GuessMarketWorldEngineTest.java" "%PROJECT_ROOT%modules\guessmarket-engine\src\test\java\guessmarket\engine\LmsrOpeningTest.java" "%PROJECT_ROOT%modules\guessmarket-engine\src\test\java\guessmarket\engine\LmsrPurchaseTest.java" "%PROJECT_ROOT%modules\guessmarket-engine\src\test\java\guessmarket\engine\LmsrSettlementTest.java" "%PROJECT_ROOT%modules\guessmarket-engine\src\test\java\guessmarket\engine\xml\ex2\Ex2XmlWorldLoaderTest.java"
if errorlevel 1 exit /b 1
set "TEST_CP=%OUTPUT%\tests;%OUTPUT%\dev\lib\guessmarket-dto.jar;%OUTPUT%\dev\lib\guessmarket-engine.jar;%OUTPUT%\dev\lib\guessmarket-javafx-ui.jar;%JAXB_CP%"
pushd "%PROJECT_ROOT%"
"%JAVA%" --module-path "%FX%" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -jar "%JUNIT%" execute --class-path "%TEST_CP%" --select-class guessmarket.engine.LmsrOpeningTest --select-class guessmarket.engine.LmsrPurchaseTest --select-class guessmarket.engine.LmsrSettlementTest --select-class guessmarket.engine.GuessMarketWorldEngineTest --select-class guessmarket.engine.xml.ex2.Ex2XmlWorldLoaderTest --select-class guessmarket.ui.javafx.WorldSessionTest --select-class guessmarket.ui.javafx.PackagedViewTest --select-class guessmarket.ui.javafx.OpeningFlowTest --select-class guessmarket.ui.javafx.LmsrLifecycleFlowTest --select-class guessmarket.ui.javafx.LmsrReviewFixesTest --fail-if-no-tests --disable-banner --details=summary --reports-dir "%OUTPUT%\reports" > "%OUTPUT%\reports\junit-output.txt" 2>&1
set "TEST_EXIT=%ERRORLEVEL%"
popd
type "%OUTPUT%\reports\junit-output.txt"
if not "%TEST_EXIT%"=="0" exit /b %TEST_EXIT%
echo SUCCESS: JavaFX development JARs and tests passed.
echo Run: "%OUTPUT%\dev\run-javafx.bat"
exit /b 0

:sources
set "SOURCE_ROOT=%~1"
set "SOURCE_LIST=%~2"
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; $files=@(Get-ChildItem -LiteralPath $env:SOURCE_ROOT -Recurse -Filter '*.java'); if($files.Count -eq 0){throw 'No Java sources'}; $lines=$files | ForEach-Object {([char]34)+$_.FullName.Replace('\','/')+([char]34)}; [IO.File]::WriteAllLines($env:SOURCE_LIST,$lines,[Text.UTF8Encoding]::new($false))"
exit /b %ERRORLEVEL%
