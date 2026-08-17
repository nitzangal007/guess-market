@echo off
setlocal EnableExtensions DisableDelayedExpansion

for %%I in ("%~dp0.") do set "PROJECT_ROOT=%%~fI"
set "BUILD_DIR=%PROJECT_ROOT%\build"
set "DTO_MODULE=%PROJECT_ROOT%\modules\guessmarket-dto"
set "ENGINE_MODULE=%PROJECT_ROOT%\modules\guessmarket-engine"
set "UI_MODULE=%PROJECT_ROOT%\modules\guessmarket-ui"
set "JUNIT_JAR=%PROJECT_ROOT%\tools\testing\junit-platform-console-standalone-6.1.1.jar"
set "JAXB_DIR=%PROJECT_ROOT%\tools\jaxb-ri-4.0.5\mod"
set "PRIVATE_README=%PROJECT_ROOT%\private\submission\assignment-1\README.pdf"

if /I "%~1"=="--verify-reports" (
    if "%~2"=="" (
        echo ERROR: --verify-reports requires a report directory.
        exit /b 1
    )
    set "REPORT_DIRECTORY=%~f2"
    call :verify_junit_reports
    exit /b %ERRORLEVEL%
)

if /I "%~1"=="--verify-manifest" (
    if "%~2"=="" (
        echo ERROR: --verify-manifest requires a manifest file.
        exit /b 1
    )
    set "MANIFEST_FILE=%~f2"
    call :verify_manifest_source
    exit /b %ERRORLEVEL%
)

call :phase "preflight"
call :preflight
if errorlevel 1 goto :failure

call :phase "clean"
call :clean
if errorlevel 1 goto :failure

call :phase "source lists"
call :source_lists
if errorlevel 1 goto :failure

call :phase "DTO compile"
call :compile_dto
if errorlevel 1 goto :failure

call :phase "Engine compile and resource copy"
call :compile_engine
if errorlevel 1 goto :failure

call :phase "UI compile"
call :compile_ui
if errorlevel 1 goto :failure

call :phase "DTO test compile"
call :dtoTests
if errorlevel 1 goto :failure

call :phase "Engine test compile"
call :engineTests
if errorlevel 1 goto :failure

call :phase "UI test compile"
call :uiTests
if errorlevel 1 goto :failure

call :phase "JUnit execution with captured output and XML reports"
call :run_junit
if errorlevel 1 goto :failure

call :phase "mandatory JUnit proof verifier"
set "REPORT_DIRECTORY=%BUILD_DIR%\reports\junit"
call :verify_junit_reports
if errorlevel 1 goto :failure

call :phase "three application JARs"
call :create_jars
if errorlevel 1 goto :failure

call :phase "JAR validation and ownership inspection"
call :validate_jars
if errorlevel 1 goto :failure

call :phase "exact staging"
call :stage_distribution
if errorlevel 1 goto :failure

call :phase "ZIP creation with jar --no-manifest"
call :create_zip
if errorlevel 1 goto :failure

call :phase "exact ZIP membership verification"
call :verify_zip
if errorlevel 1 goto :failure

call :phase "fresh extraction under a path containing spaces"
call :extract_zip
if errorlevel 1 goto :failure

call :phase "packaged-process checks"
call :packaged_process_checks
if errorlevel 1 goto :failure

call :phase "success summary"
echo SUCCESS: Guess Market Exercise 1 build, package, extraction, and process checks passed.
echo JUnit proof: %BUILD_DIR%\reports\junit-proof.txt
echo Distribution: %BUILD_DIR%\distributions\guess-market-exercise-1.zip
exit /b 0

:phase
echo.
echo === %~1 ===
exit /b 0

:preflight
if not exist "%PROJECT_ROOT%\PROJECT_HANDOFF.md" (
    echo ERROR: project root is missing PROJECT_HANDOFF.md.
    exit /b 1
)
for %%I in ("%PROJECT_ROOT%\build") do set "EXPECTED_BUILD_DIR=%%~fI"
for %%I in ("%BUILD_DIR%") do set "RESOLVED_BUILD_DIR=%%~fI"
if /I not "%EXPECTED_BUILD_DIR%"=="%RESOLVED_BUILD_DIR%" (
    echo ERROR: refusing to clean a non-project build directory.
    exit /b 1
)
if /I "%RESOLVED_BUILD_DIR%"=="%PROJECT_ROOT%" (
    echo ERROR: refusing to clean the project root.
    exit /b 1
)
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME must point to one Oracle JDK 25 installation.
    exit /b 1
)
for %%I in ("%JAVA_HOME%") do set "JDK_HOME=%%~fI"
set "JAVA_EXE=%JDK_HOME%\bin\java.exe"
set "JAVAC_EXE=%JDK_HOME%\bin\javac.exe"
set "JAR_EXE=%JDK_HOME%\bin\jar.exe"
for %%F in ("%JAVA_EXE%" "%JAVAC_EXE%" "%JAR_EXE%" "%JUNIT_JAR%" "%PRIVATE_README%") do (
    if not exist "%%~fF" (
        echo ERROR: required input is missing: %%~fF
        exit /b 1
    )
)
for %%F in (jakarta.activation-api.jar angus-activation.jar jakarta.xml.bind-api.jar jaxb-core.jar jaxb-impl.jar) do (
    if not exist "%JAXB_DIR%\%%F" (
        echo ERROR: required JAXB runtime JAR is missing: %%F
        exit /b 1
    )
)
"%JAVA_EXE%" -version 2>&1 | findstr /r /c:"25\.[0-9]" >nul
if errorlevel 1 (
    echo ERROR: JAVA_HOME does not provide Java 25.
    exit /b 1
)
"%JAVAC_EXE%" -version | findstr /b /c:"javac 25." >nul
if errorlevel 1 (
    echo ERROR: JAVA_HOME does not provide javac 25.
    exit /b 1
)
"%JAR_EXE%" --version | findstr /b /c:"jar 25." >nul
if errorlevel 1 (
    echo ERROR: JAVA_HOME does not provide jar 25.
    exit /b 1
)
set "JAXB_CP=%JAXB_DIR%\jakarta.activation-api.jar;%JAXB_DIR%\angus-activation.jar;%JAXB_DIR%\jakarta.xml.bind-api.jar;%JAXB_DIR%\jaxb-core.jar;%JAXB_DIR%\jaxb-impl.jar"
set "PATH=%JDK_HOME%\bin;%PATH%"
exit /b 0

:clean
if exist "%BUILD_DIR%" (
    rmdir /s /q "%BUILD_DIR%"
    if exist "%BUILD_DIR%" (
        echo ERROR: unable to clean the project build directory.
        exit /b 1
    )
)
mkdir "%BUILD_DIR%\sources" "%BUILD_DIR%\classes\dto" "%BUILD_DIR%\classes\engine" "%BUILD_DIR%\classes\ui" "%BUILD_DIR%\test-classes\dto" "%BUILD_DIR%\test-classes\engine" "%BUILD_DIR%\test-classes\ui" "%BUILD_DIR%\reports\junit" "%BUILD_DIR%\jars" "%BUILD_DIR%\inspection" "%BUILD_DIR%\distributions" "%BUILD_DIR%\process-input" "%BUILD_DIR%\process-state"
if errorlevel 1 exit /b 1
exit /b 0

:source_lists
call :write_source_list "%DTO_MODULE%\src\main\java" "%BUILD_DIR%\sources\dto-main.txt" || exit /b 1
call :write_source_list "%ENGINE_MODULE%\src\main\java" "%BUILD_DIR%\sources\engine-main.txt" || exit /b 1
call :write_source_list "%UI_MODULE%\src\main\java" "%BUILD_DIR%\sources\ui-main.txt" || exit /b 1
call :write_source_list "%DTO_MODULE%\src\test\java" "%BUILD_DIR%\sources\dto-test.txt" || exit /b 1
call :write_source_list "%ENGINE_MODULE%\src\test\java" "%BUILD_DIR%\sources\engine-test.txt" || exit /b 1
call :write_source_list "%UI_MODULE%\src\test\java" "%BUILD_DIR%\sources\ui-test.txt" || exit /b 1
exit /b 0

:write_source_list
setlocal EnableDelayedExpansion
for /f "delims=" %%F in ('dir /b /s "%~1\*.java"') do (
    set "SOURCE_PATH=%%F"
    >> "%~2" echo "!SOURCE_PATH:\=/!"
)
endlocal
if not exist "%~2" exit /b 1
for %%F in ("%~2") do if %%~zF EQU 0 (
    echo ERROR: no Java sources found under %~1.
    exit /b 1
)
exit /b 0

:compile_dto
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -Xlint:all -Werror -d "%BUILD_DIR%\classes\dto" @"%BUILD_DIR%\sources\dto-main.txt"
if errorlevel 1 exit /b 1
exit /b 0

:compile_engine
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -Xlint:all -Werror -cp "%BUILD_DIR%\classes\dto;%JAXB_CP%" -d "%BUILD_DIR%\classes\engine" @"%BUILD_DIR%\sources\engine-main.txt"
if errorlevel 1 exit /b 1
xcopy /e /i /y "%ENGINE_MODULE%\src\main\resources\*" "%BUILD_DIR%\classes\engine\" >nul
if errorlevel 1 exit /b 1
exit /b 0

:compile_ui
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -Xlint:all -Werror -cp "%BUILD_DIR%\classes\dto;%BUILD_DIR%\classes\engine" -d "%BUILD_DIR%\classes\ui" @"%BUILD_DIR%\sources\ui-main.txt"
if errorlevel 1 exit /b 1
exit /b 0

:dtoTests
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -Xlint:all -Werror -cp "%BUILD_DIR%\classes\dto;%JUNIT_JAR%" -d "%BUILD_DIR%\test-classes\dto" @"%BUILD_DIR%\sources\dto-test.txt"
if errorlevel 1 exit /b 1
exit /b 0

:engineTests
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -Xlint:all -Werror -cp "%BUILD_DIR%\classes\dto;%BUILD_DIR%\classes\engine;%JUNIT_JAR%;%JAXB_CP%" -d "%BUILD_DIR%\test-classes\engine" @"%BUILD_DIR%\sources\engine-test.txt"
if errorlevel 1 exit /b 1
xcopy /e /i /y "%ENGINE_MODULE%\src\test\resources\*" "%BUILD_DIR%\test-classes\engine\" >nul
if errorlevel 1 exit /b 1
exit /b 0

:uiTests
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -Xlint:all -Werror -cp "%BUILD_DIR%\classes\dto;%BUILD_DIR%\classes\engine;%BUILD_DIR%\classes\ui;%JUNIT_JAR%;%JAXB_CP%" -d "%BUILD_DIR%\test-classes\ui" @"%BUILD_DIR%\sources\ui-test.txt"
if errorlevel 1 exit /b 1
exit /b 0

:run_junit
"%JAVA_EXE%" -jar "%JUNIT_JAR%" execute --class-path "%BUILD_DIR%\test-classes\dto;%BUILD_DIR%\test-classes\engine;%BUILD_DIR%\test-classes\ui;%BUILD_DIR%\classes\dto;%BUILD_DIR%\classes\engine;%BUILD_DIR%\classes\ui;%JAXB_CP%" --scan-class-path --include-engine junit-jupiter --fail-if-no-tests --disable-ansi-colors --details=flat --reports-dir "%BUILD_DIR%\reports\junit" > "%BUILD_DIR%\reports\junit-console.txt" 2>&1
if errorlevel 1 (
    type "%BUILD_DIR%\reports\junit-console.txt"
    exit /b 1
)
exit /b 0

:verify_junit_reports
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; $reportDirectory = [IO.Path]::GetFullPath($env:REPORT_DIRECTORY); $expected = @('guessmarket.dto.DtoContractTest','guessmarket.engine.LmsrCalculatorTest','guessmarket.engine.MarketEventTest','guessmarket.engine.GuessMarketEngineUseCaseTest','guessmarket.engine.GuessMarketEngineXmlLoadTest','guessmarket.engine.xml.JaxbMarketMapperTest','guessmarket.engine.xml.XmlMarketLoaderTest','guessmarket.engine.GuessMarketEnginePersistenceTest','guessmarket.ui.console.ConsoleInputTest','guessmarket.ui.console.ConsoleRendererTest','guessmarket.ui.console.GuessMarketConsoleAppTest'); $files = @(Get-ChildItem -LiteralPath $reportDirectory -Filter 'TEST-*.xml' -File); if ($files.Count -eq 0) { throw 'No JUnit XML reports were found.' }; $documents = @($files | ForEach-Object { [xml](Get-Content -LiteralPath $_.FullName -Raw) }); foreach ($className in $expected) { $tests = @($documents | ForEach-Object { $_.SelectNodes(\"//testcase[@classname='$className']\") }); if ($tests.Count -eq 0) { throw \"Required JUnit class did not execute a test: $className\" } }; $badElements = @($documents | ForEach-Object { $_.SelectNodes('//failure|//error|//skipped|//aborted|//disabled') }); if ($badElements.Count -ne 0) { throw \"JUnit reports contain failure, error, skipped, aborted, or disabled elements: $($badElements.Count)\" }; $badAttributes = @($documents | ForEach-Object { $_.SelectNodes('//testsuite') } | Where-Object { foreach ($attributeName in @('failures','errors','skipped','disabled','aborted')) { if ($_.GetAttribute($attributeName) -match '^[1-9]') { return $true } }; return $false }); if ($badAttributes.Count -ne 0) { throw \"JUnit reports contain nonzero aggregate failure, error, skipped, disabled, or aborted counts: $($badAttributes.Count)\" }; $testCount = @($documents | ForEach-Object { $_.SelectNodes('//testcase') }).Count; if ($testCount -lt 1) { throw 'JUnit reports contain zero executed tests.' }; $summary = \"JUnit proof passed: $($expected.Count) required classes, $testCount executed tests, 0 failures, 0 container failures, 0 skips, 0 disabled tests, 0 aborts.\"; $summary | Set-Content -LiteralPath (Join-Path $reportDirectory 'junit-proof.txt') -Encoding ascii; Write-Output $summary"
exit /b %ERRORLEVEL%

:create_jars
"%JAR_EXE%" --create --file "%BUILD_DIR%\jars\guessmarket-dto.jar" -C "%BUILD_DIR%\classes\dto" .
if errorlevel 1 exit /b 1
"%JAR_EXE%" --create --file "%BUILD_DIR%\jars\guessmarket-engine.jar" -C "%BUILD_DIR%\classes\engine" .
if errorlevel 1 exit /b 1
"%JAR_EXE%" --create --file "%BUILD_DIR%\jars\guessmarket-ui.jar" --manifest "%PROJECT_ROOT%\packaging\guessmarket-ui.mf" -C "%BUILD_DIR%\classes\ui" .
if errorlevel 1 exit /b 1
exit /b 0

:validate_jars
call :inventory_jar "%BUILD_DIR%\jars\guessmarket-dto.jar" "%BUILD_DIR%\inspection\guessmarket-dto.txt" "guessmarket/dto/" || exit /b 1
call :inventory_jar "%BUILD_DIR%\jars\guessmarket-engine.jar" "%BUILD_DIR%\inspection\guessmarket-engine.txt" "guessmarket/engine/" || exit /b 1
call :inventory_jar "%BUILD_DIR%\jars\guessmarket-ui.jar" "%BUILD_DIR%\inspection\guessmarket-ui.txt" "guessmarket/ui/console/" || exit /b 1
findstr /i /r /c:"Test\.class" /c:"org/junit/" /c:"jakarta/" /c:"com/sun/" "%BUILD_DIR%\inspection\guessmarket-dto.txt" "%BUILD_DIR%\inspection\guessmarket-engine.txt" "%BUILD_DIR%\inspection\guessmarket-ui.txt" >nul
if not errorlevel 1 (
    echo ERROR: an application JAR contains a test, JUnit, or unpacked vendor class.
    exit /b 1
)
findstr /x /c:"guessmarket/engine/xml/GM-EX1-Schema.xsd" "%BUILD_DIR%\inspection\guessmarket-engine.txt" >nul
if errorlevel 1 (
    echo ERROR: Engine JAR is missing the trusted XSD.
    exit /b 1
)
if exist "%BUILD_DIR%\inspection\ui-manifest" rmdir /s /q "%BUILD_DIR%\inspection\ui-manifest"
mkdir "%BUILD_DIR%\inspection\ui-manifest"
if errorlevel 1 exit /b 1
pushd "%BUILD_DIR%\inspection\ui-manifest"
"%JAR_EXE%" --extract --file "%BUILD_DIR%\jars\guessmarket-ui.jar" META-INF/MANIFEST.MF
if errorlevel 1 (
    popd
    exit /b 1
)
popd
set "MANIFEST_FILE=%PROJECT_ROOT%\packaging\guessmarket-ui.mf"
call :verify_manifest_source
if errorlevel 1 exit /b 1
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$lines = @(Get-Content -LiteralPath '%BUILD_DIR%\inspection\ui-manifest\META-INF\MANIFEST.MF'); if ($lines -notcontains 'Main-Class: guessmarket.ui.console.ConsoleMain') { throw 'UI JAR Main-Class is incorrect.' }; $index = [Array]::FindIndex([string[]]$lines, [Predicate[string]]{ param($line) $line.StartsWith('Class-Path: ') }); if ($index -lt 0) { throw 'UI JAR is missing Class-Path.' }; $classPath = $lines[$index].Substring('Class-Path: '.Length); while ($index + 1 -lt $lines.Count -and $lines[$index + 1].StartsWith(' ')) { $index++; $classPath += $lines[$index].Substring(1) }; $expected = 'lib/guessmarket-engine.jar lib/guessmarket-dto.jar lib/jakarta.activation-api.jar lib/angus-activation.jar lib/jakarta.xml.bind-api.jar lib/jaxb-core.jar lib/jaxb-impl.jar'; if ($classPath -ne $expected) { throw ('UI JAR Class-Path mismatch: ' + $classPath) }"
if errorlevel 1 exit /b 1
exit /b 0

:verify_manifest_source
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$raw = [IO.File]::ReadAllText($env:MANIFEST_FILE); $actual = $raw.Replace(\"`r`n\", \"`n\"); if ($actual.Contains(\"`r\")) { throw 'The source manifest uses a bare carriage return.' }; $expected = (@('Manifest-Version: 1.0','Main-Class: guessmarket.ui.console.ConsoleMain','Class-Path: lib/guessmarket-engine.jar lib/guessmarket-dto.jar','  lib/jakarta.activation-api.jar lib/angus-activation.jar','  lib/jakarta.xml.bind-api.jar lib/jaxb-core.jar lib/jaxb-impl.jar','') -join \"`n\") + \"`n\"; if ($actual -cne $expected) { throw 'The source manifest does not match the required logical text and final blank line.' }; Write-Output 'Source manifest logical text passed.'"
exit /b %ERRORLEVEL%

:inventory_jar
"%JAR_EXE%" --list --file "%~1" > "%~2"
if errorlevel 1 exit /b 1
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$entries = Get-Content -LiteralPath '%~2'; $prefix = '%~3'; $parts = $prefix.TrimEnd('/').Split('/'); $parents = for ($index = 1; $index -lt $parts.Count; $index++) { (($parts[0..($index - 1)] -join '/') + '/') }; $invalid = @($entries | Where-Object { $_ -ne 'META-INF/' -and $_ -ne 'META-INF/MANIFEST.MF' -and $_ -notin $parents -and -not $_.StartsWith($prefix) }); if ($invalid.Count -ne 0) { throw ('Unexpected JAR entry: ' + ($invalid -join ', ')) }"
exit /b %ERRORLEVEL%

:stage_distribution
set "STAGING_DIR=%BUILD_DIR%\staging"
mkdir "%STAGING_DIR%\lib"
if errorlevel 1 exit /b 1
copy /y "%PROJECT_ROOT%\packaging\run.bat" "%STAGING_DIR%\run.bat" >nul || exit /b 1
copy /y "%PRIVATE_README%" "%STAGING_DIR%\README.pdf" >nul || exit /b 1
copy /y "%BUILD_DIR%\jars\guessmarket-ui.jar" "%STAGING_DIR%\guessmarket-ui.jar" >nul || exit /b 1
copy /y "%BUILD_DIR%\jars\guessmarket-engine.jar" "%STAGING_DIR%\lib\guessmarket-engine.jar" >nul || exit /b 1
copy /y "%BUILD_DIR%\jars\guessmarket-dto.jar" "%STAGING_DIR%\lib\guessmarket-dto.jar" >nul || exit /b 1
for %%F in (jakarta.activation-api.jar angus-activation.jar jakarta.xml.bind-api.jar jaxb-core.jar jaxb-impl.jar) do (
    copy /y "%JAXB_DIR%\%%F" "%STAGING_DIR%\lib\%%F" >nul || exit /b 1
)
exit /b 0

:create_zip
set "ZIP_FILE=%BUILD_DIR%\distributions\guess-market-exercise-1.zip"
pushd "%BUILD_DIR%\staging"
"%JAR_EXE%" --create --file "%ZIP_FILE%" --no-manifest .
if errorlevel 1 (
    popd
    exit /b 1
)
popd
exit /b 0

:verify_zip
"%JAR_EXE%" --list --file "%BUILD_DIR%\distributions\guess-market-exercise-1.zip" > "%BUILD_DIR%\inspection\guess-market-exercise-1-zip.txt"
if errorlevel 1 exit /b 1
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$actual = @(Get-Content -LiteralPath '%BUILD_DIR%\inspection\guess-market-exercise-1-zip.txt' | Where-Object { $_ -ne '' }); $expected = @('run.bat','README.pdf','guessmarket-ui.jar','lib/','lib/guessmarket-engine.jar','lib/guessmarket-dto.jar','lib/jakarta.activation-api.jar','lib/angus-activation.jar','lib/jakarta.xml.bind-api.jar','lib/jaxb-core.jar','lib/jaxb-impl.jar'); $difference = Compare-Object -ReferenceObject $expected -DifferenceObject $actual; if ($difference) { throw ('ZIP membership mismatch: ' + (($difference | ForEach-Object { $_.InputObject }) -join ', ')) }"
exit /b %ERRORLEVEL%

:extract_zip
set "EXTRACTION_DIR=%BUILD_DIR%\verification\Fresh Extraction With Spaces"
if exist "%EXTRACTION_DIR%" rmdir /s /q "%EXTRACTION_DIR%"
mkdir "%EXTRACTION_DIR%"
if errorlevel 1 exit /b 1
pushd "%EXTRACTION_DIR%"
"%JAR_EXE%" --extract --file "%BUILD_DIR%\distributions\guess-market-exercise-1.zip"
if errorlevel 1 (
    popd
    exit /b 1
)
popd
for %%F in ("%EXTRACTION_DIR%\run.bat" "%EXTRACTION_DIR%\README.pdf" "%EXTRACTION_DIR%\guessmarket-ui.jar" "%EXTRACTION_DIR%\lib\guessmarket-engine.jar" "%EXTRACTION_DIR%\lib\guessmarket-dto.jar") do if not exist "%%~fF" exit /b 1
exit /b 0

:packaged_process_checks
set "EXTRACTION_DIR=%BUILD_DIR%\verification\Fresh Extraction With Spaces"
copy /y "%ENGINE_MODULE%\src\test\resources\guessmarket\engine\xml\fixtures\supplied\multiple.xml" "%BUILD_DIR%\process-input\multiple.xml" >nul || exit /b 1
copy /y "%ENGINE_MODULE%\src\test\resources\guessmarket\engine\xml\fixtures\supplied\error-3.xml" "%BUILD_DIR%\process-input\error-3.xml" >nul || exit /b 1
> "%BUILD_DIR%\process-input\inside-exit.txt" echo 8
pushd "%EXTRACTION_DIR%"
call ".\run.bat" < "%BUILD_DIR%\process-input\inside-exit.txt" > "%BUILD_DIR%\reports\packaged-inside-exit.txt" 2>&1
if errorlevel 1 (
    popd
    exit /b 1
)
popd
call "%EXTRACTION_DIR%\run.bat" < "%BUILD_DIR%\process-input\inside-exit.txt" > "%BUILD_DIR%\reports\packaged-outside-exit.txt" 2>&1
if errorlevel 1 exit /b 1
(
    echo 1
    echo %BUILD_DIR%\process-input\multiple.xml
    echo.
    echo 2
    echo.
    echo 4
    echo 1
    echo 1
    echo 1
    echo.
    echo 5
    echo 1
    echo 1
    echo.
    echo 6
    echo %BUILD_DIR%\process-state\session
    echo.
    echo 8
) > "%BUILD_DIR%\process-input\integration.txt"
pushd "%EXTRACTION_DIR%"
call ".\run.bat" < "%BUILD_DIR%\process-input\integration.txt" > "%BUILD_DIR%\reports\packaged-integration.txt" 2>&1
if errorlevel 1 (
    popd
    exit /b 1
)
popd
(
    echo 7
    echo %BUILD_DIR%\process-state\session
    echo.
    echo 3
    echo 1
    echo.
    echo 8
) > "%BUILD_DIR%\process-input\restore.txt"
pushd "%EXTRACTION_DIR%"
call ".\run.bat" < "%BUILD_DIR%\process-input\restore.txt" > "%BUILD_DIR%\reports\packaged-restore.txt" 2>&1
if errorlevel 1 (
    popd
    exit /b 1
)
popd
(
    echo 1
    echo %BUILD_DIR%\process-input\error-3.xml
    echo.
    echo 8
) > "%BUILD_DIR%\process-input\invalid-xml.txt"
pushd "%EXTRACTION_DIR%"
call ".\run.bat" < "%BUILD_DIR%\process-input\invalid-xml.txt" > "%BUILD_DIR%\reports\packaged-invalid-xml.txt" 2>&1
if errorlevel 1 (
    popd
    exit /b 1
)
popd
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$checks = @(@('%BUILD_DIR%\reports\packaged-inside-exit.txt','Goodbye.'),@('%BUILD_DIR%\reports\packaged-outside-exit.txt','Goodbye.'),@('%BUILD_DIR%\reports\packaged-integration.txt','System loaded successfully.'),@('%BUILD_DIR%\reports\packaged-integration.txt','PURCHASE SUMMARY'),@('%BUILD_DIR%\reports\packaged-integration.txt','Event closed successfully.'),@('%BUILD_DIR%\reports\packaged-integration.txt','System state saved successfully.'),@('%BUILD_DIR%\reports\packaged-restore.txt','System state restored successfully.'),@('%BUILD_DIR%\reports\packaged-restore.txt','Status: CLOSED'),@('%BUILD_DIR%\reports\packaged-invalid-xml.txt','Error: The XML contains invalid market data in XML event 2, field comision.')); foreach ($check in $checks) { if (-not (Select-String -LiteralPath $check[0] -Pattern $check[1] -SimpleMatch -Quiet)) { throw ('Packaged-process transcript is missing: ' + $check[1]) } }; if (Select-String -LiteralPath '%BUILD_DIR%\reports\packaged-invalid-xml.txt' -Pattern 'Exception' -SimpleMatch -Quiet) { throw 'Expected XML recovery exposed an exception type.' }"
if errorlevel 1 exit /b 1
exit /b 0

:failure
echo ERROR: build stopped during a required phase.
exit /b 1
