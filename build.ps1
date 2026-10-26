<#
.SYNOPSIS
    Сборка исполняемого jar-файла ProAX.

.DESCRIPTION
    Ищет Maven в PATH, а если его нет — использует локальную копию из
    %USERPROFILE%\.m2\wrapper\dists (Maven Wrapper). По умолчанию сборка идёт
    в офлайн-режиме, чтобы не ждать недоступную сеть; ключ -Online отключает это.

.EXAMPLE
    .\build.ps1
    .\build.ps1 -Online
#>
[CmdletBinding()]
param(
    [switch]$Online,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Resolve-Maven {
    $cmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $candidates = Get-ChildItem -Path (Join-Path $env:USERPROFILE '.m2\wrapper\dists') -Filter 'mvn.cmd' -Recurse -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending
    if ($candidates) { return $candidates[0].FullName }

    throw 'Maven не найден. Установите Maven или добавьте его в PATH (https://maven.apache.org/download.cgi).'
}

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { return $env:JAVA_HOME }
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) { return (Split-Path (Split-Path $java.Source -Parent) -Parent) }
    throw 'Java 21 не найдена. Установите JDK 21 и задайте JAVA_HOME.'
}

$maven = Resolve-Maven
$env:JAVA_HOME = Resolve-JavaHome

$arguments = @('-B', '-f', (Join-Path $root 'pom.xml'))
if (-not $Online) { $arguments += '-o' }
if ($SkipTests) { $arguments += '-DskipTests' }
$arguments += 'package'

Write-Host "Maven: $maven"
Write-Host "Java:  $env:JAVA_HOME"
Write-Host "Сборка ProAX…"

& $maven @arguments
if ($LASTEXITCODE -ne 0) { throw "Сборка завершилась с кодом $LASTEXITCODE" }

Write-Host "Готово: $(Join-Path $root 'target\proax.jar')"
