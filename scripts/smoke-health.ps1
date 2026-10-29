param(
    [Parameter(Mandatory=$false)]
    [string]$GatewayUrl = "http://localhost:8088"
)

Write-Host "=== Smoke Test: Health Checks ===" -ForegroundColor Cyan
Write-Host ""

$endpoints = @(
    @{Name="Tamagochirest /actuator/health"; Url="http://localhost:8080/actuator/health"},
    @{Name="Tamagochirest /livez"; Url="http://localhost:8080/livez"},
    @{Name="Tamagochirest /readyz"; Url="http://localhost:8080/readyz"},
    @{Name="Gateway /actuator/health"; Url="$GatewayUrl/actuator/health"},
    @{Name="Gateway /livez"; Url="$GatewayUrl/livez"},
    @{Name="Gateway /readyz"; Url="$GatewayUrl/readyz"}
)

$allOk = $true

foreach ($endpoint in $endpoints) {
    try {
        $response = Invoke-WebRequest -Uri $endpoint.Url -Method Get -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ $($endpoint.Name): OK" -ForegroundColor Green
        } else {
            Write-Host "✗ $($endpoint.Name): Status $($response.StatusCode)" -ForegroundColor Red
            $allOk = $false
        }
    } catch {
        Write-Host "✗ $($endpoint.Name): FAILED - $_" -ForegroundColor Red
        $allOk = $false
    }
}

Write-Host ""
if ($allOk) {
    Write-Host "=== All health checks passed ===" -ForegroundColor Green
    exit 0
} else {
    Write-Host "=== Some health checks failed ===" -ForegroundColor Red
    exit 1
}
