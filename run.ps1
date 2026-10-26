<#
.SYNOPSIS
    Запуск ProAX: при необходимости собирает jar и стартует приложение.

.EXAMPLE
    .\run.ps1
    .\run.ps1 -Port 9090
#>
[CmdletBinding()]
param(
    [int]$Port = 8080,
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Join-Path $root 'target\proax.jar'

if ($Rebuild -or -not (Test-Path $jar)) {
    & (Join-Path $root 'build.ps1') -SkipTests
}

$java = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java -ErrorAction Stop).Source
}

Write-Host "ProAX запускается: http://127.0.0.1:$Port"
Write-Host 'Демо-доступ: demo@proax.ru / demo123 (компания), admin@proax.ru / admin123 (админ)'
Write-Host 'Остановка — Ctrl+C.'

& $java -jar $jar "--server.port=$Port"
