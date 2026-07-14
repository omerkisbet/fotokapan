# Wildlife Archive — Müşteri Girişi ve Fotokapan Yetkilendirme Patch'i

Bu patch, Camera Management + Jitsi MVP sürümünün üzerine uygulanır.

## Eklenen özellikler

- Spring Security tabanlı form girişi ve sunucu taraflı oturum.
- MongoDB `users` koleksiyonu.
- `ADMIN` ve `CUSTOMER` rolleri.
- BCrypt parola hashleme.
- CSRF koruması.
- Müşterinin yalnızca kendisine atanmış fotokapanları görmesi.
- Canlı yayın ve medya arşivinin kamera yetkisine göre filtrelenmesi.
- Müşterilerin medya yükleyememesi veya kayıt silememesi.
- Müşteri modunda Jitsi yayıncı seçeneğinin kapatılması.
- Yönetici için müşteri oluşturma ve fotokapan atama ekranı.
- Demo yönetici ve müşteri hesabı.

## Uygulama

ZIP'i ayrı bir klasöre çıkarın. PowerShell'de:

```powershell
Set-ExecutionPolicy -Scope Process Bypass

& "C:\PATCH_KLASORU\apply-auth-patch.ps1" `
  -ProjectPath "C:\Users\utkuk\IdeaProjects\wildlife-media-service"
```

Script mevcut dosyaları şu klasöre yedekler:

```text
patch-backup-customer-auth-YYYYMMDD-HHMMSS
```

Mevcut `compose.yaml` dosyasını tamamen değiştirmez. Yalnızca `app.environment` bölümüne hesap değişkenlerini eklemeye çalışır; MediaMTX veya diğer servisler korunur.

## Başlatma

```powershell
cd C:\Users\utkuk\IdeaProjects\wildlife-media-service
docker compose up --build -d
docker compose ps
docker compose logs --tail=150 app
```

Tarayıcı:

```text
http://localhost:8081/login.html
```

## Demo hesapları

Yönetici:

```text
E-posta: admin@wildlife.local
Parola: Admin123!
```

Müşteri:

```text
E-posta: customer@wildlife.local
Parola: Customer123!
```

İlk başlatmada `CAM-TR-001`, demo müşteriye otomatik atanır.

Canlı ortama geçmeden önce `.env` dosyasındaki parolaları değiştirin:

```properties
APP_ADMIN_EMAIL=admin@wildlife.local
APP_ADMIN_PASSWORD=Guclu-Yeni-Parola
APP_CUSTOMER_EMAIL=customer@wildlife.local
APP_CUSTOMER_PASSWORD=Farkli-Guclu-Parola
```

Parola environment değişkenleri yalnızca kullanıcı ilk kez oluşturulurken kullanılır. Kullanıcı MongoDB'de zaten varsa `.env` değişikliği mevcut parola hashini otomatik değiştirmez.

## Ekranlar

```text
/login.html  -> Müşteri/yönetici girişi
/            -> Yetkiye göre filtrelenmiş arşiv
/live.html   -> Yetkiye göre filtrelenmiş Jitsi canlı gözlem
/admin.html  -> Müşteri oluşturma ve kamera atama (yalnızca ADMIN)
```

## API davranışı

- `GET /api/cameras`: Yönetici için tüm kameralar, müşteri için yalnızca atanmış kameralar.
- `GET /api/media`: Müşteri için yalnızca atanmış kameraların kayıtları.
- `GET /api/media/{id}/content`: Medya kaydının kamera sahipliği doğrulanır.
- `POST /api/media`: Yalnızca yönetici.
- `DELETE /api/media/{id}`: Yalnızca yönetici.
- `POST /api/admin/customers`: Müşteri hesabı oluşturur.
- `PATCH /api/cameras/{cameraCode}/customer`: Kamerayı müşteriye atar.

## Kontrol

Yönetici hesabıyla giriş yapın:

1. `Yönetim` sayfasını açın.
2. Yeni bir müşteri oluşturun.
3. `CAM-TR-001` veya başka bir kamerayı müşteriye atayın.
4. Çıkış yapın.
5. Müşteri hesabıyla giriş yapın.
6. Yalnızca atanmış kameranın ve ona ait medya kayıtlarının göründüğünü doğrulayın.

## Not

`meet.jit.si` odası MVP aşamasında dış Jitsi servisi üzerinde çalışır. Spring Boot tarafındaki kamera ve arşiv yetkilendirmesi uygulanmıştır; ancak tahmin edilebilir oda adlarına karşı tam Jitsi izolasyonu için sonraki aşamada self-hosted Jitsi/JaaS ve JWT kullanılmalıdır.
