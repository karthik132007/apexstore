$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Load-Settings.ps1')
$driver = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.local\m2\org\postgresql\postgresql') -Filter 'postgresql-*.jar' -Recurse | Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $driver) { throw 'Build the project first so the PostgreSQL JDBC driver is available.' }
& java -cp $driver.FullName (Join-Path $PSScriptRoot 'DatabaseSetup.java')
if ($LASTEXITCODE -ne 0) { throw 'Database setup failed.' }
