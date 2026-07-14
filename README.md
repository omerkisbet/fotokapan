# Wildlife Media Service — Jitsi ve Müşteri Yetkilendirmeli Sürüm

Fotokapanlardan gelen fotoğraf ve kısa videoları saklayan, geçmiş kayıtları görüntüleyen ve Jitsi üzerinden canlı gözlem sağlayan Spring Boot uygulamasıdır.

## Teknoloji yığını

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data MongoDB
- Spring Security
- MongoDB 8
- Docker ve Docker Compose
- Vanilla HTML, CSS ve JavaScript
- Jitsi Meet IFrame API

## Güncel özellikler

- JPG, PNG, WEBP ve MP4 yükleme
- Medya dosyalarının kalıcı Docker volume içinde saklanması
- Metadata kayıtlarının MongoDB’de tutulması
- Kamera ve medya türüne göre filtreleme
- Fotoğraf görüntüleme, video oynatma, indirme ve silme
- Fotokapan oluşturma, listeleme, durum güncelleme ve müşteri atama
- `ONLINE`, `OFFLINE` ve `MAINTENANCE` kamera durumları
- Jitsi tabanlı canlı yayıncı ve izleyici ekranı
- MongoDB tabanlı kullanıcı hesapları
- `ADMIN` ve `CUSTOMER` rolleri
- BCrypt parola hashleme
- Session tabanlı form girişi
- CSRF koruması
- Her müşterinin yalnızca kendisine atanmış kameraları ve medya kayıtlarını görmesi
- Yönetici için müşteri ve kamera yönetim ekranı
- Actuator health kontrolü

## Çalıştırma

Proje klasöründe PowerShell açın:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
docker compose logs --tail=120 app
```

Uygulama:

```text
http://localhost:8081
```

Health kontrolü:

```text
http://localhost:8081/actuator/health
```

## Varsayılan kullanıcılar

İlk başlangıçta environment değişkenlerinden iki hesap oluşturulur.

### Yönetici

```text
E-posta: admin@wildlife.local
Parola: Admin123!
```

### Müşteri

```text
E-posta: customer@wildlife.local
Parola: Customer123!
```

Canlı ortamdan önce `.env` dosyasındaki parolaları değiştirin.

## Sayfalar

```text
/login.html   Giriş ekranı
/             Medya arşivi
/live.html    Jitsi canlı gözlem
/admin.html   Müşteri ve kamera yönetimi
```

## Jitsi canlı yayın testi

Yönetici hesabıyla yayıncı modu:

```text
http://localhost:8081/live.html?camera=CAM-TR-001&mode=publisher
```

Müşteri veya başka bir tarayıcıyla izleyici modu:

```text
http://localhost:8081/live.html?camera=CAM-TR-001&mode=viewer
```

Bu geliştirme sürümü `meet.jit.si` kullanır. Production ortamında self-hosted Jitsi veya JaaS, JWT doğrulaması ve kontrollü oda erişimi kullanılmalıdır.

## MongoDB Compass

Varsayılan host portu `27019`’dur.

```text
mongodb://wildlife:change-this-password@localhost:27019/wildlife_media?authSource=admin
```

`.env` içindeki kullanıcı adı veya parola değiştirildiyse bağlantı URI’sini de aynı değerlere göre güncelleyin.

## Önemli Docker notu

Uygulama container’ı MongoDB’ye şu adresle bağlanır:

```text
mongodb:27017
```

`localhost:27019` yalnızca Windows üzerinden MongoDB Compass bağlantısı içindir.

Servisleri verileri koruyarak durdurmak için:

```powershell
docker compose down
```

Tüm MongoDB ve medya volume’lerini de silmek için:

```powershell
docker compose down -v
```

`-v` seçeneği kalıcı verileri siler.

## Temel API uçları

```text
GET    /api/auth/me
GET    /api/auth/csrf
GET    /api/cameras
GET    /api/cameras/{cameraCode}
POST   /api/cameras
PATCH  /api/cameras/{cameraCode}/status
DELETE /api/cameras/{cameraCode}
GET    /api/media
POST   /api/media
GET    /api/media/{id}/content
GET    /api/media/{id}/download
DELETE /api/media/{id}
GET    /api/admin/customers
POST   /api/admin/customers
PATCH  /api/admin/cameras/{cameraCode}/customer
```

## Veri saklama

- MongoDB kayıtları: `wildlife-mongo-data` volume
- Fotoğraf ve video dosyaları: `wildlife-media-files` volume
- Kullanıcı parolaları: MongoDB’de BCrypt hash olarak
- Jitsi oda adı: Kamera kaydı içinde `jitsiRoomName` alanında
