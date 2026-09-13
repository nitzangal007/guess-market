@echo off
setlocal EnableExtensions DisableDelayedExpansion
if not exist "%~dp0runtime\bin\java.exe" (
    echo ERROR: Extract the complete archive, including runtime.
    exit /b 1
)
if not exist "%~dp0lib\guessmarket-javafx-ui.jar" (
    echo ERROR: Extract the complete archive, including lib.
    exit /b 1
)
"%~dp0runtime\bin\java.exe" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp "%~dp0lib\*" guessmarket.ui.javafx.GuessMarketApplication
exit /b %ERRORLEVEL%
