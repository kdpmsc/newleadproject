# Use -WithOllama when placing type=aiagent calls so the Ollama container is started.
param(
    [switch]$WithOllama
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Import-DotEnv {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return
    }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $name, $value = $line.Split("=", 2)
        $name = $name.Trim()
        $value = $value.Trim().Trim("'").Trim('"')
        if ($name) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

function Get-NgrokHttpsUrl {
    try {
        $tunnels = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -TimeoutSec 2
        $https = $tunnels.tunnels | Where-Object { $_.public_url -like "https://*" } | Select-Object -First 1
        if ($https) {
            return $https.public_url.TrimEnd("/")
        }
    } catch {
        return $null
    }
    return $null
}

Import-DotEnv (Join-Path $root ".env")

if (-not $env:TWILIO_ACCOUNT_SID -or -not $env:TWILIO_AUTH_TOKEN -or -not $env:TWILIO_PHONE_NUMBER) {
    Write-Host "Create a .env file from .env.example and set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_PHONE_NUMBER."
    Write-Host "The app can still start, but Twilio will not place outbound calls until those values are set."
}

if (-not (Get-Command ngrok -ErrorAction SilentlyContinue)) {
    throw "ngrok is not installed or not on PATH. Install it from https://ngrok.com/download"
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is not installed or not on PATH."
}

$existingUrl = Get-NgrokHttpsUrl
if (-not $existingUrl) {
    Write-Host "Starting ngrok tunnel for http://localhost:8080 ..."
    $ngrokArgs = @("http", "8080")
    if ($env:NGROK_DOMAIN) {
        $ngrokArgs = @("http", "--url=$($env:NGROK_DOMAIN)", "8080")
    }
    Start-Process -FilePath "ngrok" -ArgumentList $ngrokArgs -WindowStyle Minimized | Out-Null
}

$publicUrl = $null
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    $publicUrl = Get-NgrokHttpsUrl
    if ($publicUrl) {
        break
    }
}

if (-not $publicUrl) {
    throw "Could not read the ngrok public URL from http://127.0.0.1:4040. Open the ngrok window and confirm you are signed in."
}

$env:TWILIO_APP_BASE_URL = $publicUrl
$env:APP_PUBLIC_BASE_URL = $publicUrl
$env:FRONTEND_ORIGIN = $publicUrl
$env:CORS_ALLOWED_ORIGINS = $publicUrl
Write-Host "Public URL: $publicUrl"
Write-Host "Twilio will call: $publicUrl/api/v1/voice/property-qualification"

Write-Host "Starting Postgres..."
docker compose up -d postgres | Out-Host

if ($WithOllama) {
    Write-Host "Starting Ollama (this can take several minutes on first run)..."
    docker compose up -d ollama | Out-Host
}

Write-Host "Building and starting the app (without waiting for Ollama)..."
docker compose up -d --build --no-deps app | Out-Host

Write-Host "Waiting for http://localhost:8080/actuator/health ..."
$healthy = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2
        if ($health.status -eq "UP") {
            $healthy = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 3
    }
}

if (-not $healthy) {
    Write-Host "The app has not reported healthy yet. Check logs with: docker logs -f leadproject-app"
} else {
    Write-Host "App is up."
}

Write-Host ""
Write-Host "Local:     http://localhost:8080"
Write-Host "Public:    $publicUrl"
Write-Host "Swagger:   $publicUrl/swagger-ui.html"
Write-Host "Dashboard: $publicUrl/"
Write-Host "Ngrok UI:  http://127.0.0.1:4040"
Write-Host ""
Write-Host "If you change the ngrok URL, re-run this script so APP_PUBLIC_BASE_URL / TWILIO_APP_BASE_URL are updated and the app container is recreated."
Write-Host "Place a call with POST $publicUrl/api/v1/leads/call after signing in (admin / admin123)."
