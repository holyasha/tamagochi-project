param(
    [Parameter(Mandatory=$false)]
    [int]$Count = 30,

    [Parameter(Mandatory=$false)]
    [int]$IntervalMs = 200,

    [Parameter(Mandatory=$false)]
    [string]$GatewayUrl = "http://localhost:8088"
)

Write-Host "Authenticating with Keycloak..." -ForegroundColor Cyan

$tokenUrl = "http://localhost:8085/realms/course/protocol/openid-connect/token"
$tokenBody = @{
    grant_type = "client_credentials"
    client_id = "course-service"
    client_secret = "course-service-secret"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded" -ErrorAction Stop
    $token = $tokenResponse.access_token
    Write-Host "✓ Authentication successful" -ForegroundColor Green
} catch {
    Write-Host "✗ Authentication failed: $_" -ForegroundColor Red
    exit 1
}

# Create owner
Write-Host "Creating test owner..." -ForegroundColor Cyan
$ownerUrl = "$GatewayUrl/api/owners"
$ownerBody = @{
    name = "Test Owner $(Get-Date -Format 'HHmmss')"
    birthDate = "1990-01-01"
} | ConvertTo-Json

$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    $owner = Invoke-RestMethod -Uri $ownerUrl -Method Post -Headers $headers -Body $ownerBody -ErrorAction Stop
    Write-Host "✓ Owner created: $($owner.id)" -ForegroundColor Green
} catch {
    Write-Host "✗ Owner creation failed: $_" -ForegroundColor Red
    exit 1
}

# Create tamagochi
Write-Host "Creating test tamagochi..." -ForegroundColor Cyan
$tamagochiUrl = "$GatewayUrl/api/tamagochis"
$tamagochiBody = @{
    name = "Test Tamagochi"
    species = "Dragon"
    color = "Blue"
    birthDate = "2026-01-01"
    ownerId = $owner.id
} | ConvertTo-Json

try {
    $tamagochi = Invoke-RestMethod -Uri $tamagochiUrl -Method Post -Headers $headers -Body $tamagochiBody -ErrorAction Stop
    $tamagochiId = $tamagochi.id
    Write-Host "✓ Tamagochi created: $tamagochiId" -ForegroundColor Green
} catch {
    Write-Host "✗ Tamagochi creation failed: $_" -ForegroundColor Red
    exit 1
}

# Generate traffic
Write-Host "`nGenerating $Count requests with ${IntervalMs}ms interval..." -ForegroundColor Cyan

$statuses = @{}
$errors = 0

for ($i = 1; $i -le $Count; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "$tamagochiUrl/$tamagochiId" -Headers $headers -Method Get -ErrorAction Stop
        $status = $response.StatusCode
        if ($statuses.ContainsKey($status)) {
            $statuses[$status]++
        } else {
            $statuses[$status] = 1
        }
        Write-Host "." -NoNewline -ForegroundColor Green
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($statuses.ContainsKey($status)) {
            $statuses[$status]++
        } else {
            $statuses[$status] = 1
        }
        Write-Host "!" -NoNewline -ForegroundColor Red
        $errors++
    }

    if ($i -lt $Count) {
        Start-Sleep -Milliseconds $IntervalMs
    }
}

Write-Host "`n`n✓ Traffic generation completed" -ForegroundColor Green
Write-Host "Requests: $Count"
Write-Host "Statuses: $($statuses | ConvertTo-Json -Compress)"
Write-Host "Errors: $errors"

return @{
    Requests = $Count
    Statuses = $statuses
    Errors = $errors
}
