param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectPath
)

$ErrorActionPreference = "Stop"

function Write-Utf8NoBom {
    param(
        [string]$Path,
        [string]$Content
    )

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

$ProjectPath = (Resolve-Path $ProjectPath).Path
$PatchRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$PayloadRoot = Join-Path $PatchRoot "payload"
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$BackupRoot = Join-Path $ProjectPath "patch-backup-customer-auth-$Timestamp"

if (-not (Test-Path (Join-Path $ProjectPath "pom.xml") -PathType Leaf)) {
    throw "Seçilen klasörde pom.xml bulunamadı: $ProjectPath"
}

New-Item -ItemType Directory -Path $BackupRoot -Force | Out-Null
Write-Host "Yedek klasörü: $BackupRoot" -ForegroundColor Cyan

# Yeni ve değiştirilmiş kaynak dosyalarını kopyala.
Get-ChildItem $PayloadRoot -Recurse -File | ForEach-Object {
    $relativePath = $_.FullName.Substring($PayloadRoot.Length).TrimStart([char[]]"\/")
    $targetPath = Join-Path $ProjectPath $relativePath
    $targetDirectory = Split-Path -Parent $targetPath

    if (Test-Path $targetPath -PathType Leaf) {
        $backupPath = Join-Path $BackupRoot $relativePath
        $backupDirectory = Split-Path -Parent $backupPath
        New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
        Copy-Item $targetPath $backupPath -Force
    }

    New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
    Copy-Item $_.FullName $targetPath -Force
    Write-Host "Güncellendi: $relativePath"
}

# pom.xml içine Spring Security bağımlılıklarını ekle.
$pomPath = Join-Path $ProjectPath "pom.xml"
$pom = [string](Get-Content $pomPath -Raw)

if ($pom -notmatch "spring-boot-starter-security") {
    $webMvcPattern = '(?s)<dependency>\s*<groupId>org\.springframework\.boot</groupId>\s*<artifactId>spring-boot-starter-webmvc</artifactId>\s*</dependency>'
    $webMvcMatch = [regex]::Match($pom, $webMvcPattern)

    if (-not $webMvcMatch.Success) {
        throw "pom.xml içinde spring-boot-starter-webmvc bloğu bulunamadı. PATCH_README_TR.md içindeki manuel adımı uygulayın."
    }

    $securityDependency = @'

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
'@
    $pom = $pom.Insert($webMvcMatch.Index + $webMvcMatch.Length, $securityDependency)
}

if ($pom -notmatch "spring-security-test") {
    $webMvcTestPattern = '(?s)<dependency>\s*<groupId>org\.springframework\.boot</groupId>\s*<artifactId>spring-boot-starter-webmvc-test</artifactId>\s*<scope>test</scope>\s*</dependency>'
    $webMvcTestMatch = [regex]::Match($pom, $webMvcTestPattern)

    if ($webMvcTestMatch.Success) {
        $securityTestDependency = @'

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
'@
        $pom = $pom.Insert($webMvcTestMatch.Index + $webMvcTestMatch.Length, $securityTestDependency)
    }
}

Write-Utf8NoBom -Path $pomPath -Content $pom

# application.properties içine bootstrap hesap ayarlarını ekle.
$applicationPropertiesPath = Join-Path $ProjectPath "src/main/resources/application.properties"
$applicationProperties = [string](Get-Content $applicationPropertiesPath -Raw)
if ($applicationProperties -notmatch "app\.bootstrap\.admin-email") {
    $applicationProperties += Get-Content (Join-Path $PatchRoot "application-auth-addition.properties") -Raw
    Write-Utf8NoBom -Path $applicationPropertiesPath -Content $applicationProperties
}

# Mevcut tasarımı silmeden yalnızca oturum/yönetim CSS bölümünü ekle.
$stylesPath = Join-Path $ProjectPath "src/main/resources/static/styles.css"
$styles = [string](Get-Content $stylesPath -Raw)
if ($styles -notmatch "OTURUM, MÜŞTERİ GÖRÜNÜMÜ VE GİRİŞ EKRANI") {
    $styles += "`r`n`r`n" + (Get-Content (Join-Path $PatchRoot "styles-auth-addition.css") -Raw)
    Write-Utf8NoBom -Path $stylesPath -Content $styles
}

# compose.yaml içindeki app environment bölümüne hesap değişkenlerini ekle.
$composePath = Join-Path $ProjectPath "compose.yaml"
if (Test-Path $composePath -PathType Leaf) {
    $composeLines = Get-Content $composePath
    if (-not ($composeLines -match "APP_ADMIN_EMAIL:")) {
        $updatedLines = New-Object System.Collections.Generic.List[string]
        $inserted = $false

        foreach ($line in $composeLines) {
            $updatedLines.Add($line)
            if (-not $inserted -and $line -match "^(\s*)MEDIA_STORAGE_PATH:") {
                $indent = $Matches[1]
                $updatedLines.Add($indent + 'APP_ADMIN_EMAIL: ${APP_ADMIN_EMAIL:-admin@wildlife.local}')
                $updatedLines.Add($indent + 'APP_ADMIN_PASSWORD: ${APP_ADMIN_PASSWORD:-Admin123!}')
                $updatedLines.Add($indent + 'APP_CUSTOMER_EMAIL: ${APP_CUSTOMER_EMAIL:-customer@wildlife.local}')
                $updatedLines.Add($indent + 'APP_CUSTOMER_PASSWORD: ${APP_CUSTOMER_PASSWORD:-Customer123!}')
                $inserted = $true
            }
        }

        if ($inserted) {
            Write-Utf8NoBom -Path $composePath -Content (($updatedLines -join "`r`n") + "`r`n")
        } else {
            Write-Warning "compose.yaml içinde MEDIA_STORAGE_PATH satırı bulunamadı. Hesap environment değişkenlerini elle ekleyin."
        }
    }
}

# .env ve .env.example içine varsayılan demo hesaplarını ekle; mevcut değerleri değiştirme.
$envEntries = [ordered]@{
    "APP_ADMIN_EMAIL" = "admin@wildlife.local"
    "APP_ADMIN_PASSWORD" = "Admin123!"
    "APP_CUSTOMER_EMAIL" = "customer@wildlife.local"
    "APP_CUSTOMER_PASSWORD" = "Customer123!"
}

foreach ($envFileName in @(".env", ".env.example")) {
    $envPath = Join-Path $ProjectPath $envFileName

    if (-not (Test-Path $envPath -PathType Leaf)) {
        if ($envFileName -eq ".env" -and (Test-Path (Join-Path $ProjectPath ".env.example") -PathType Leaf)) {
            Copy-Item (Join-Path $ProjectPath ".env.example") $envPath
        } else {
            New-Item -ItemType File -Path $envPath -Force | Out-Null
        }
    }

    $envContent = [string](Get-Content $envPath -Raw)
    foreach ($entry in $envEntries.GetEnumerator()) {
        if ($envContent -notmatch "(?m)^$([regex]::Escape($entry.Key))=") {
            if ($envContent.Length -gt 0 -and -not $envContent.EndsWith("`n")) {
                $envContent += "`r`n"
            }
            $envContent += "$($entry.Key)=$($entry.Value)`r`n"
        }
    }
    Write-Utf8NoBom -Path $envPath -Content $envContent
}

Write-Host ""
Write-Host "Müşteri girişi ve kamera yetkilendirme patch'i uygulandı." -ForegroundColor Green
Write-Host "Şimdi proje klasöründe çalıştırın:" -ForegroundColor Yellow
Write-Host "docker compose up --build -d"
Write-Host ""
Write-Host "Demo yönetici: admin@wildlife.local / Admin123!"
Write-Host "Demo müşteri: customer@wildlife.local / Customer123!"
Write-Host "Canlı ortamdan önce .env parolalarını mutlaka değiştirin." -ForegroundColor Yellow
