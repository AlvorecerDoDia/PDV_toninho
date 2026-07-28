[CmdletBinding()]
param(
    [switch]$Installer
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $projectRoot
try {
    & ".\mvnw.cmd" clean package
    if ($LASTEXITCODE -ne 0) {
        throw "A compilação Maven falhou."
    }

    $packageInput = Join-Path $projectRoot "target\package-input"
    New-Item -ItemType Directory -Path $packageInput -Force | Out-Null
    & ".\mvnw.cmd" dependency:copy-dependencies `
        "-DincludeScope=runtime" `
        "-DoutputDirectory=$packageInput"
    if ($LASTEXITCODE -ne 0) {
        throw "Não foi possível preparar as dependências do aplicativo."
    }
    Copy-Item -LiteralPath (Join-Path $projectRoot "target\pdv-toninho.jar") `
        -Destination $packageInput -Force

    $jpackage = $null
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
        if (Test-Path -LiteralPath $candidate) {
            $jpackage = $candidate
        }
    }
    if (-not $jpackage) {
        $command = Get-Command "jpackage.exe" -ErrorAction SilentlyContinue
        if ($command) {
            $jpackage = $command.Source
        }
    }
    if (-not $jpackage) {
        throw "jpackage.exe não encontrado. Instale um JDK 21 e configure JAVA_HOME."
    }

    $packageType = if ($Installer) { "exe" } else { "app-image" }
    $arguments = @(
        "--type", $packageType,
        "--name", "PDV Toninho",
        "--app-version", "1.0.0",
        "--vendor", "PDV Toninho",
        "--description", "Sistema local de ponto de venda",
        "--dest", (Join-Path $projectRoot "target\distribution"),
        "--input", $packageInput,
        "--main-jar", "pdv-toninho.jar",
        "--main-class", "br.com.loja.pdv.PdvLauncher",
        "--java-options", "-Dfile.encoding=UTF-8"
    )
    if ($Installer) {
        $arguments += @(
            "--win-dir-chooser",
            "--win-menu",
            "--win-menu-group", "PDV Toninho",
            "--win-shortcut"
        )
    }

    & $jpackage @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "O jpackage não conseguiu gerar o pacote Windows."
    }

    if ($Installer) {
        Write-Host "Instalador criado em target\distribution."
    } else {
        Write-Host "Aplicativo portátil criado em target\distribution\PDV Toninho."
    }
} finally {
    Pop-Location
}
