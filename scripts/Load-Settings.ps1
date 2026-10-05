$projectRoot = Split-Path -Parent $PSScriptRoot
$settingsPath = Join-Path $projectRoot '.local\settings.json'
if (-not (Test-Path -LiteralPath $settingsPath)) { throw 'Run scripts/Configure-Local.ps1 first.' }
$settings = Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
foreach ($property in $settings.PSObject.Properties) {
    $value = [string]$property.Value
    if ($property.Name -like '*_DB_URL' -and $value -notmatch 'sslfactory=') { $value += '&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory' }
    [Environment]::SetEnvironmentVariable($property.Name, $value, 'Process')
}
