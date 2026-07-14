# Fotokapan Yönetimi ve MongoDB Tabanlı Jitsi Odaları Patch'i

Bu patch, `wildlife-media-service-jitsi-mvp` sürümünün üzerine uygulanır.

## Eklenen özellikler

- MongoDB `cameras` koleksiyonu
- `Camera` modeli ve `CameraStatus` enum'u
- Camera repository, service ve REST controller
- Fotokapan oluşturma, listeleme, getirme, durum değiştirme ve silme endpointleri
- Jitsi oda adının frontend içinde sabit tutulması yerine MongoDB'den okunması
- Ana sayfada aktif fotokapan listesi
- Her fotokapan için **Canlı izle** ve **Arşivi aç** işlemleri
- Medya yükleme ve filtreleme alanlarında kayıtlı fotokapan önerileri
- İlk çalıştırmada `CAM-TR-001` demo kaydının otomatik oluşturulması
- Request validation ve anlaşılır API hata cevapları

## Uygulama

Patch ZIP'ini herhangi bir klasöre çıkartın. Proje klasöründe PowerShell açın ve patch klasöründeki script'i çalıştırın:

```powershell
& "C:\PATCH_KLASORU\apply-patch.ps1" -ProjectPath "C:\Users\utkuk\IdeaProjects\wildlife-media-service"
```

Patch klasörünü doğrudan proje köküne çıkardıysanız:

```powershell
.\apply-patch.ps1
```

Ardından uygulamayı yeniden build edin:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs --tail=120 app
```

MongoDB volume'ünü silmek gerekmez. Uygulama başladıktan sonra demo fotokapan otomatik eklenir.

## Endpointler

### Tüm fotokapanlar

```powershell
curl.exe http://localhost:8081/api/cameras
```

### Yalnızca aktif fotokapanlar

```powershell
curl.exe "http://localhost:8081/api/cameras?activeOnly=true"
```

### Yeni fotokapan oluşturma

```powershell
curl.exe -X POST http://localhost:8081/api/cameras `
  -H "Content-Type: application/json" `
  -d '{"cameraCode":"CAM-TR-002","name":"Orman Girişi","location":"Samsun","status":"ONLINE","description":"Kuzey saha noktası","active":true}'
```

`jitsiRoomName` gönderilmezse oda adı kamera kodundan otomatik oluşturulur.

### Tek fotokapan

```powershell
curl.exe http://localhost:8081/api/cameras/CAM-TR-001
```

### Durum güncelleme

```powershell
curl.exe -X PATCH http://localhost:8081/api/cameras/CAM-TR-001/status `
  -H "Content-Type: application/json" `
  -d '{"status":"MAINTENANCE"}'
```

Geçerli durumlar:

```text
ONLINE
OFFLINE
MAINTENANCE
```

### Fotokapan silme

```powershell
curl.exe -X DELETE http://localhost:8081/api/cameras/CAM-TR-002
```

## Arayüz

Ana sayfa:

```text
http://localhost:8081
```

Canlı gözlem:

```text
http://localhost:8081/live.html?camera=CAM-TR-001&mode=viewer
```

Yayıncı modu:

```text
http://localhost:8081/live.html?camera=CAM-TR-001&mode=publisher
```

## Not

Bu aşamada `status` alanı yönetilen bir metadata değeridir. Jitsi odasında gerçekten yayıncı olup olmadığını otomatik algılayan heartbeat mekanizması sonraki geliştirme aşamasıdır.
