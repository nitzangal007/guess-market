@echo off
setlocal EnableExtensions DisableDelayedExpansion
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0packaging\build-exercise3.ps1" -Target "%~1"
exit /b %ERRORLEVEL%
