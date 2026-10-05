foreach ($port in @(8080,8081,8082,8083,8084)) {
    try { $health = Invoke-RestMethod -Uri "http://localhost:$port/actuator/health" -TimeoutSec 3; Write-Host "Port ${port}: $($health.status)" }
    catch { Write-Host "Port ${port}: not ready" }
}
