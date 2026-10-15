@echo off
setlocal EnableExtensions DisableDelayedExpansion
if not exist "%~dp0lib\guessmarket-client.jar" (
 echo ERROR: Extract the complete client folder before launching.
 exit /b 1
)
set "JAVA=java.exe"
if not "%JAVA_HOME%"=="" if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if exist "%~dp0runtime\bin\java.exe" set "JAVA=%~dp0runtime\bin\java.exe"
"%JAVA%" --module-path "%~dp0javafx\lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp "%~dp0lib\*" guessmarket.client.ClientMain
exit /b %ERRORLEVEL%
