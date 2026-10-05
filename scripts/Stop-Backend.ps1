$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$statePath = Join-Path $projectRoot '.local\processes.json'
if (-not (Test-Path -LiteralPath $statePath)) { Write-Host 'No recorded backend processes.'; return }
$records = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
foreach ($entry in $records) {
    $process = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
    if ($process -and $process.ProcessName -eq 'java' -and $process.StartTime.ToUniversalTime().Ticks -eq ([datetime]$entry.startedAt).ToUniversalTime().Ticks) {
        $expectedJar = Join-Path $projectRoot "$($entry.name)\target\$($entry.name)-1.0.0.jar"
        Get-CimInstance Win32_Process -Filter "ParentProcessId=$($process.Id)" | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine.Contains($expectedJar) } | ForEach-Object { Stop-Process -Id $_.ProcessId }
        Stop-Process -Id $process.Id
        Write-Host "$($entry.name) stopped."
    }
}
