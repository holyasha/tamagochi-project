param(
    [Parameter(Mandatory=$false)]
    [string]$GatewayUrl = "http://localhost:8088"
)

Write-Host "=== Tamagochi Observability Stack Verification ===" -ForegroundColor Cyan
Write-Host ""

$allOk = $true

# Check infrastructure
Write-Host "Checking infrastructure..." -ForegroundColor Cyan

$services = @(
    @{Name="Postgres"; Url="http://localhost:5432"},
    @{Name="Keycloak"; Url="http://localhost:8085/realms/course/.well-known/openid-configuration"},
    @{Name="Redis"; Url="redis://localhost:6379"},
    @{Name="Tempo"; Url="http://localhost:3200/ready"},
    @{Name="Loki"; Url="http://localhost:3100/ready"},
    @{Name="Prometheus"; Url="http://localhost:9090/-/ready"},
    @{Name="Grafana"; Url="http://localhost:3000/api/health"},
    @{Name="Alertmanager"; Url="http://localhost:9093/-/ready"}
)

foreach ($svc in $services) {
    if ($svc.Url -notlike "redis://*") {
        try {
            $response = Invoke-WebRequest -Uri $svc.Url -Method Get -TimeoutSec 5 -ErrorAction Stop
            Write-Host "  ✓ $($svc.Name): OK" -ForegroundColor Green
        } catch {
            Write-Host "  ✗ $($svc.Name): FAILED" -ForegroundColor Red
            $allOk = $false
        }
    } else {
        Write-Host "  ⊙ $($svc.Name): skipped (manual check needed)" -ForegroundColor Yellow
    }
}

# Check Prometheus targets
Write-Host "`nChecking Prometheus targets..." -ForegroundColor Cyan
try {
    $targets = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/targets" -ErrorAction Stop
    $activeTargets = $targets.data.activeTargets | Where-Object { $_.health -eq "up" }
    Write-Host "  ✓ Active targets: $($activeTargets.Count)" -ForegroundColor Green

    $expectedJobs = @("tamagochirest", "gateway-service")
    foreach ($job in $expectedJobs) {
        $found = $activeTargets | Where-Object { $_.labels.job -eq $job }
        if ($found) {
            Write-Host "    ✓ $job: UP" -ForegroundColor Green
        } else {
            Write-Host "    ✗ $job: DOWN" -ForegroundColor Red
            $allOk = $false
        }
    }
} catch {
    Write-Host "  ✗ Failed to query Prometheus targets" -ForegroundColor Red
    $allOk = $false
}

# Check metrics endpoints
Write-Host "`nChecking metrics endpoints..." -ForegroundColor Cyan
$metricsEndpoints = @(
    @{Name="tamagochirest"; Url="http://localhost:9080/actuator/prometheus"},
    @{Name="gateway-service"; Url="http://localhost:9088/actuator/prometheus"}
)

foreach ($endpoint in $metricsEndpoints) {
    try {
        $response = Invoke-WebRequest -Uri $endpoint.Url -ErrorAction Stop
        if ($response.Content -match "http_server_requests") {
            Write-Host "  ✓ $($endpoint.Name): metrics available" -ForegroundColor Green
        } else {
            Write-Host "  ⚠ $($endpoint.Name): metrics found but http_server_requests missing" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ✗ $($endpoint.Name): FAILED" -ForegroundColor Red
        $allOk = $false
    }
}

# Check Grafana datasources
Write-Host "`nChecking Grafana datasources..." -ForegroundColor Cyan
try {
    $datasources = Invoke-RestMethod -Uri "http://admin:admin-dev-only@localhost:3000/api/datasources" -ErrorAction Stop
    $expectedDS = @("Prometheus", "Tempo", "Loki")
    foreach ($ds in $expectedDS) {
        $found = $datasources | Where-Object { $_.name -eq $ds }
        if ($found) {
            Write-Host "  ✓ $ds: configured" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $ds: NOT FOUND" -ForegroundColor Red
            $allOk = $false
        }
    }
} catch {
    Write-Host "  ✗ Failed to query Grafana datasources" -ForegroundColor Red
    $allOk = $false
}

Write-Host ""
if ($allOk) {
    Write-Host "=== All checks passed ===" -ForegroundColor Green
    return @{Status="OK"}
} else {
    Write-Host "=== Some checks failed ===" -ForegroundColor Red
    return @{Status="FAILED"}
}
