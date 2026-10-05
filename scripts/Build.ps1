param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $mavenArguments = @('-B', "-Dmaven.repo.local=$projectRoot\.local\m2", 'verify')
    if ($SkipTests) { $mavenArguments += '-DskipTests' }
    & mvn @mavenArguments
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
} finally { Pop-Location }
