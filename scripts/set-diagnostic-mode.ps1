param(
    [Parameter(Mandatory=$false)]
    [ValidateSet('NORMAL', 'SLOW', 'FAIL')]
    [string]$Mode = 'NORMAL',

    [Parameter(Mandatory=$false)]
    [ValidateRange(1, 120)]
    [int]$Seconds = 60
)

$url = "http://localhost:8080/internal/diagnostic/mode?mode=$Mode&seconds=$Seconds"

try {
    $response = Invoke-RestMethod -Uri $url -Method Post -ErrorAction Stop
    Write-Host "✓ Diagnostic mode set to $Mode for $Seconds seconds" -ForegroundColor Green
    return $response
} catch {
    Write-Host "✗ Failed to set diagnostic mode: $_" -ForegroundColor Red
    exit 1
}
