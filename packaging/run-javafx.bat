@echo off
setlocal EnableExtensions DisableDelayedExpansion
if "%JAVA_HOME%"=="" (
    echo ERROR: Set JAVA_HOME to Oracle JDK 25.0.4.
    exit /b 1
)
if "%JAVAFX_HOME%"=="" (
    echo ERROR: Set JAVAFX_HOME to the JavaFX 25.0.4 SDK directory.
    exit /b 1
)
if not exist "%~dp0lib\guessmarket-javafx-ui.jar" (
    echo ERROR: Run the development launcher copied by build-javafx.bat.
    exit /b 1
)
"%JAVA_HOME%\bin\java.exe" --module-path "%JAVAFX_HOME%\lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp "%~dp0lib\*" guessmarket.ui.javafx.GuessMarketApplication
exit /b %ERRORLEVEL%
