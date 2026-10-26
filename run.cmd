@echo off
rem Запуск ProAX в один клик (Windows).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
