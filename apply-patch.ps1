param(
    [string]$ProjectPath = (Get-Location).Path
)

$ErrorActionPreference = "Stop"
$resolvedProjectPath = (Resolve-Path $ProjectPath).Path
$pomPath = Join-Path $resolvedProjectPath "pom.xml"
$sourcePath = Join-Path $PSScriptRoot "patch-files"

if (-not (Test-Path $pomPath -PathType Leaf)) {
    throw "Hedef klasörde pom.xml bulunamadı: $resolvedProjectPath"
}

if (-not (Test-Path $sourcePath -PathType Container)) {
    throw "Patch dosyaları bulunamadı: $sourcePath"
}

Copy-Item -Path (Join-Path $sourcePath "*") -Destination $resolvedProjectPath -Recurse -Force

Write-Host "Camera Management patch başarıyla uygulandı." -ForegroundColor Green
Write-Host "Şimdi şu komutu çalıştırın:" -ForegroundColor Cyan
Write-Host "docker compose up --build -d"
