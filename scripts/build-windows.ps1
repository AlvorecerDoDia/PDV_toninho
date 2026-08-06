[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $projectRoot
try {
    # Compila e testa antes de preparar o aplicativo portatil.
    & ".\mvnw.cmd" clean package
    if ($LASTEXITCODE -ne 0) {
        throw "A compilacao Maven falhou."
    }

    $packageInput = Join-Path $projectRoot "target\package-input"
    New-Item -ItemType Directory -Path $packageInput -Force | Out-Null

    # Copia as dependencias usadas durante a execucao.
    & ".\mvnw.cmd" dependency:copy-dependencies `
        "-DincludeScope=runtime" `
        "-DoutputDirectory=$packageInput"
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel preparar as dependencias do aplicativo."
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
        throw "jpackage.exe nao encontrado. Instale um JDK 21 e configure JAVA_HOME."
    }

    & $jpackage `
        --type app-image `
        --name "PDV Toninho" `
        --app-version "1.0.0" `
        --vendor "PDV Toninho" `
        --description "Sistema local de ponto de venda" `
        --dest (Join-Path $projectRoot "target\distribution") `
        --input $packageInput `
        --main-jar "pdv-toninho.jar" `
        --main-class "br.com.loja.pdv.PdvLauncher" `
        --java-options "-Dfile.encoding=UTF-8"

    if ($LASTEXITCODE -ne 0) {
        throw "O jpackage nao conseguiu gerar o aplicativo portatil."
    }

    Write-Host "Aplicativo criado em target\distribution\PDV Toninho."
} finally {
    Pop-Location
}
