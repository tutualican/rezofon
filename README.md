# Rezofon

**JAM+ Ses Atölyesi** — Bluetooth hoparlörün için sistem geneli 10 bant ekolayzer, bas/sub kontrolü ve
15 dakika uyku sayacına karşı hat bekçisi.

Android (Kotlin + Jetpack Compose) ve Windows (C# / WinForms) olarak iki nativ uygulama.
Şarkı çalar değildir: sesi işler, bağlantıyı yönetir, cihazı ölçer.

> Rezofon · rezonans + fon · `studio.rezofon.app`

---

## Neden var?

Grundig JAM+ 3.5 W RMS mono sürücü ve pasif radyatörlü, **kendi DSP'si olmayan** bir Bluetooth
hoparlördür. Üreticinin mobil uygulaması desteği kaldırıldı, herkese açık bir ürün yazılımı (OTA)
kanalı yok. Yani hoparlörün içine yazı yazmanın bir yolu yok — ama sesin *hoparlöre girmeden önce*
işlenmesinin yolu var. Rezofon tam olarak bunu yapar ve yapamadığını da açıkça söyler.

## Ne gerçekten çalışıyor?

| Özellik | Android | Windows |
| --- | --- | --- |
| 10 bant parametrik ekolayzer (±12 dB) | `android.media.audiofx.Equalizer` | **Equalizer APO** köprüsü |
| Bas vurgusu / punch | `BassBoost` | APO alçak raf filtresi |
| Sub eşiği (0…+9 dB) | eğriye katlanır | APO `PK` filtresi |
| 62 Hz yüksek geçiren sürücü koruması | her zaman açık | her zaman açık (`HPQ Fc 62 Hz`) |
| Otomatik ön kazanç (kırpma koruması) | var | var |
| Sistem sesi / sessize alma | — | Core Audio (`IAudioEndpointVolume`) |
| Hat bekçisi (otomatik kapanma engeli) | sessiz `AudioTrack` + ön plan servisi | döngüsel sessiz WAV |
| Cihaz / uç nokta teşhisi | `BluetoothAdapter` + `AudioManager` | Core Audio uç noktaları + WMI |
| Profil paylaşımı | JSON dışa/içe aktarma | JSON panoya/dosyaya |
| Test sinyali (EQ doğrulama) | log tarama 40 Hz → 16 kHz, 62 Hz, 1 kHz | — |

## Ne çalışmıyor (ve neden saklanmıyor)

- **Hoparlörün içine EQ yazmak yok.** JAM+ üzerinde DSP yok; tüm işleme telefon/PC tarafında.
- **Firmware güncellemesi yok.** Üretici bu model için OTA yayınlamıyor. Arayüzde sahte bir
  "güncelle" düğmesi yerine sürücü/uç nokta/kodek bilgisi gösterilir.
- **Sistem geneli etki kısıtlanabilir.** Android 10+ bazı üreticilerde `session 0` etkisini
  engeller. Rezofon bunu gizlemez; "sistem etkisi: kısıtlı" olarak raporlar ve oturum moduna düşer.
- **Kodek seçimi uygulamada değil.** A2DP kodeği (SBC/AAC/aptX) Android Bluetooth yığını yönetir;
  uygulama yalnızca durumu okur.

## Teknik gerçekler

- Hoparlör: 3.5 W RMS, mono, pasif radyatör, ~100 Hz – 16 kHz, Bluetooth 5.0.
- 100 Hz altı fiziksel olarak sürücüyle değil radyatörle taşınır; bu yüzden "bas" 100–250 Hz
  gövdesinde duyulur. Eğriler bu sınır bilinerek ayarlandı.
- Uyku sayacı hoparlörün ürün yazılımındadır; Rezofon ona müdahale etmez, yalnızca ses akışını
  açık tutarak sayacın sıfırlanmasını sağlar. Garanti ve pil sağlığı etkilenmez.

## Depo yapısı

```
android/                  Kotlin + Jetpack Compose
  app/src/main/java/studio/rezofon/app/
    audio/                SoundEngine, GuardService, ToneGenerator, DeviceInspector, AudioModel
    state/                AppState, Prefs
    ui/                   Root, Home, Eq, Bass, Device, Guard, Workshop, Settings, Components
    ui/theme/             palet, tipografi, yay (spring) specleri
desktop/                  C# / WinForms, harici bağımlılık yok
  src/Native.cs           Core Audio COM interop
  src/ApoBridge.cs        Equalizer APO köprüsü (geri alınabilir)
  src/Catalog.cs          presetler, ön kazanç, JSON
  src/Ui.cs               GDI+ tema, tile, eğri, ikon
  src/RezofonForm.cs      ekranlar + etkileşim
  build.ps1               csc.exe ile tek dosya EXE üretimi
docs/                     ekran görüntüleri
```

## Kurulum

### Android
```bash
cd android
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```
veya sürümlerden hazır `app-debug.apk` dosyasını indir. minSdk 26, targetSdk 36.

### Windows
```powershell
cd desktop
powershell -ExecutionPolicy Bypass -File build.ps1   # dist\Rezofon.exe
```
.NET Framework 4.8 (Windows 10/11'de hazır) dışında bağımlılık yok. Sistem geneli EQ için
[Equalizer APO](https://sourceforge.net/projects/equalizerapo/) kurulu olmalı; Rezofon kullanıcının
`config.txt` dosyasını **ezmez**, sonuna işaretli bir `Include: rezofon.txt` bloğu ekler ve tek
tıkla bayt bayt aynı hâline döndürür.

## Tasarım

Bento grid + Material 3 Expressive, "Expressive Spatial Spring" hareket dili: her dokunuş yay
fiziğiyle tepki verir, sayfa geçişleri yaylanır, splash 1 saniyenin altında biter.
Koyu palet: `#08090B` zemin, lime `#C8FF3D`, amber `#FFB03A`, cyan `#7BE8DC`, violet `#BCA8FF`.
Space Grotesk başlık, Manrope gövde.

## Profil formatı

```json
{
  "format": "rezofon.profile/1",
  "gains": [4.5, 3.5, 2.0, 0.5, -0.5, 0.0, 0.5, 1.0, 1.5, 2.0],
  "bass": 0.30,
  "sub": 3.0,
  "loudness": 0.20,
  "preamp": -5.4,
  "enabled": true
}
```
Android ve Windows aynı biçimi okur; masaüstünde tasarlayıp telefona yapıştırabilirsiniz.

## Gizlilik

Ağ bağlantısı yok, analitik yok, reklam yok, veri toplama yok. Profiller yalnızca cihazda.

## Lisans

MIT — bkz. [LICENSE](LICENSE).

---

## English

**Rezofon** is a system-wide 10-band EQ, bass/sub shaper and idle-shutdown guard for Bluetooth
speakers. Two native apps: Kotlin/Compose for Android, dependency-free C#/WinForms for Windows.

It is **not** a music player. The JAM+ has no internal DSP and no public firmware channel, so all
processing happens on the phone/PC. The app says so instead of pretending: no fake firmware
updater, no invented codec control, and when Android blocks global audio effects it reports the
limitation rather than hiding it.

Windows applies EQ through Equalizer APO, appending a marked include-block to `config.txt` and
restoring the file byte-for-byte on removal. Android uses `android.media.audiofx` and keeps a
silent `AudioTrack` foreground service alive to defeat the ~15-minute idle power-off.

MIT licensed.
