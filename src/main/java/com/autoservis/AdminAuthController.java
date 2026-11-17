package com.autoservis;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;
import java.util.Locale;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.Instant;
import java.time.Duration;
import java.io.IOException;

@Controller
public class AdminAuthController {
    private static final Pattern EPOSTA_BICIMI = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Duration YONETICI_KURTARMA_KOD_SURESI = Duration.ofMinutes(10);
    private static final Duration YONETICI_KURTARMA_YENIDEN_GONDERME = Duration.ofSeconds(60);
    private static final int YONETICI_KURTARMA_EN_COK_DENEME = 5;

    private final IsletmeHesabiRepository hesapRepository;
    private final AracRepository aracRepository;
    private final ParolaKodlayici parolaKodlayici;
    private final IzinliEpostaRepository izinliEpostaRepository;
    private final SistemYoneticiErisimi yoneticiErisimi;
    private final HesapOnayTalebiRepository onayRepository;
    private final HesapOnayEpostaServisi onayEpostaServisi;
    private final SecureRandom random = new SecureRandom();
    private static final String KURTARMA_HASH = "yoneticiKurtarmaKodOzeti";
    private static final String KURTARMA_SON_KULLANMA = "yoneticiKurtarmaSonKullanma";
    private static final String KURTARMA_GONDERILME = "yoneticiKurtarmaGonderilme";
    private static final String KURTARMA_DENEME = "yoneticiKurtarmaDeneme";
    private static final String KURTARMA_EPOSTA = "yoneticiKurtarmaEpostasi";
    private static final String KURTARMA_TOKEN = "yoneticiKurtarmaFormToken";

    public AdminAuthController(IsletmeHesabiRepository hesapRepository,
                               AracRepository aracRepository,
                               ParolaKodlayici parolaKodlayici,
                               IzinliEpostaRepository izinliEpostaRepository,
                               SistemYoneticiErisimi yoneticiErisimi,
                               HesapOnayTalebiRepository onayRepository,
                               HesapOnayEpostaServisi onayEpostaServisi) {
        this.hesapRepository = hesapRepository;
        this.aracRepository = aracRepository;
        this.parolaKodlayici = parolaKodlayici;
        this.izinliEpostaRepository = izinliEpostaRepository;
        this.yoneticiErisimi = yoneticiErisimi;
        this.onayRepository = onayRepository;
        this.onayEpostaServisi = onayEpostaServisi;
    }

    @GetMapping("/giris")
    public String giris() {
        return "admin-giris";
    }

    @GetMapping("/kayit")
    public String kayit() {
        return "admin-kayit";
    }

    @GetMapping("/yonetici-hesap-kurtarma")
    public String yoneticiHesapKurtarma(HttpSession session, Model model) {
        kurtarmaFormunuHazirla(session, model);
        return "yonetici-hesap-kurtarma";
    }

    @PostMapping("/yonetici-hesap-kurtarma-kodu")
    public String yoneticiKurtarmaKoduGonder(@RequestParam String eposta, @RequestParam String token,
                                              HttpSession session, Model model) {
        if (!kurtarmaFormTokeniGecerli(token, session)) {
            model.addAttribute("hata", "Form süresi doldu. Sayfayı yenileyip yeniden deneyin.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }
        String temizEposta = eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);
        if (!yoneticiErisimi.yoneticiEpostasiMi(temizEposta)) {
            model.addAttribute("hata", "Bu e-posta ana yönetici e-postası olarak tanımlı değil.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }
        if (hesapRepository.existsByEposta(temizEposta)) {
            model.addAttribute("hata", "Ana yönetici hesabı zaten mevcut. Giriş ekranından parola sıfırlamayı kullanın.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }

        Instant now = Instant.now();
        Instant lastSent = (Instant) session.getAttribute(KURTARMA_GONDERILME);
        if (lastSent != null && lastSent.isAfter(now.minus(YONETICI_KURTARMA_YENIDEN_GONDERME))) {
            session.setAttribute(KURTARMA_EPOSTA, temizEposta);
            model.addAttribute("hata", "Yeni kod istemeden önce 60 saniye bekleyin.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }

        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        session.setAttribute(KURTARMA_HASH, parolaKodlayici.kodla(code));
        session.setAttribute(KURTARMA_SON_KULLANMA, now.plus(YONETICI_KURTARMA_KOD_SURESI));
        session.setAttribute(KURTARMA_GONDERILME, now);
        session.setAttribute(KURTARMA_DENEME, 0);
        session.setAttribute(KURTARMA_EPOSTA, temizEposta);
        try {
            onayEpostaServisi.yoneticiHesapKurtarmaKoduGonder(code);
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            kurtarmaKodunuTemizle(session);
            model.addAttribute("hata", "Kurtarma kodu gönderilemedi. E-posta ayarlarını kontrol edin.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }
        return "redirect:/yonetici-hesap-kurtarma?kodGonderildi";
    }

    @PostMapping("/yonetici-hesap-geri-yukle")
    @Transactional
    public String yoneticiHesabiniGeriYukle(@RequestParam String kod, @RequestParam String parola,
                                            @RequestParam String parolaTekrar, @RequestParam String token,
                                            HttpServletRequest request, HttpSession session, Model model) {
        if (!kurtarmaFormTokeniGecerli(token, session)) {
            model.addAttribute("hata", "Form süresi doldu. Sayfayı yenileyip yeniden deneyin.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }
        if (parola == null || parola.length() < 8 || parola.length() > 128) {
            model.addAttribute("hata", "Yeni parola en az 8, en fazla 128 karakter olmalı.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }
        if (!parola.equals(parolaTekrar)) {
            model.addAttribute("hata", "Parolalar aynı değil. Tekrar kontrol edin.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }

        String adminEpostasi = (String) session.getAttribute(KURTARMA_EPOSTA);
        String kodOzeti = (String) session.getAttribute(KURTARMA_HASH);
        Instant expiresAt = (Instant) session.getAttribute(KURTARMA_SON_KULLANMA);
        Integer attempts = (Integer) session.getAttribute(KURTARMA_DENEME);
        if (adminEpostasi == null || kodOzeti == null || expiresAt == null
                || expiresAt.isBefore(Instant.now()) || (attempts != null && attempts >= YONETICI_KURTARMA_EN_COK_DENEME)) {
            kurtarmaKodunuTemizle(session);
            model.addAttribute("hata", "Kodun süresi doldu veya deneme sınırı aşıldı. Yeni kod isteyin.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }
        if (!yoneticiErisimi.yoneticiEpostasiMi(adminEpostasi) || hesapRepository.existsByEposta(adminEpostasi)) {
            kurtarmaKodunuTemizle(session);
            model.addAttribute("hata", "Yönetici hesabı zaten mevcut. Giriş ekranından devam edin.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }
        if (kod == null || !kod.matches("[0-9]{6}") || !parolaKodlayici.dogrula(kod, kodOzeti)) {
            session.setAttribute(KURTARMA_DENEME, (attempts == null ? 0 : attempts) + 1);
            model.addAttribute("hata", "Doğrulama kodu hatalı.");
            kurtarmaFormunuHazirla(session, model);
            return "yonetici-hesap-kurtarma";
        }

        IsletmeHesabi admin = new IsletmeHesabi();
        admin.setIsletmeAdi("Sistem Yöneticisi");
        admin.setEposta(adminEpostasi);
        admin.setParolaOzeti(parolaKodlayici.kodla(parola));
        admin.setOnaylandi(true);
        admin.setAktif(true);
        admin = hesapRepository.saveAndFlush(admin);
        izinliEpostaRepository.findByEposta(adminEpostasi).ifPresent(izinliEpostaRepository::delete);
        kurtarmaKodunuTemizle(session);
        oturumAc(request, admin);
        return "redirect:/";
    }

    private void kurtarmaFormunuHazirla(HttpSession session, Model model) {
        String token = (String) session.getAttribute(KURTARMA_TOKEN);
        if (token == null) {
            token = java.util.UUID.randomUUID().toString();
            session.setAttribute(KURTARMA_TOKEN, token);
        }
        model.addAttribute("kurtarmaToken", token);
        model.addAttribute("kodIstenmis", session.getAttribute(KURTARMA_HASH) != null);
        model.addAttribute("kurtarmaEpostasi", session.getAttribute(KURTARMA_EPOSTA));
    }

    private boolean kurtarmaFormTokeniGecerli(String token, HttpSession session) {
        String expected = (String) session.getAttribute(KURTARMA_TOKEN);
        return expected != null && token != null && MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8), token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void kurtarmaKodunuTemizle(HttpSession session) {
        session.removeAttribute(KURTARMA_HASH);
        session.removeAttribute(KURTARMA_SON_KULLANMA);
        session.removeAttribute(KURTARMA_GONDERILME);
        session.removeAttribute(KURTARMA_DENEME);
        session.removeAttribute(KURTARMA_EPOSTA);
    }

    @PostMapping("/kayit-yap")
    @Transactional
    public String kayitYap(@RequestParam String isletmeAdi,
                           @RequestParam String eposta,
                           @RequestParam String telefon,
                           @RequestParam String parola,
                           @RequestParam String parolaTekrar,
                           HttpServletRequest request,
                           Model model) {
        String temizIsletmeAdi = isletmeAdi == null ? "" : isletmeAdi.trim();
        String temizEposta = eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);
        String temizTelefon = telefon == null ? "" : telefon.trim();
        String telefonRakamSayisi = temizTelefon.replaceAll("\\D", "");

        if (temizIsletmeAdi.isBlank() || temizIsletmeAdi.length() > 120
                || !EPOSTA_BICIMI.matcher(temizEposta).matches()
                || telefonRakamSayisi.length() < 10 || telefonRakamSayisi.length() > 15) {
            model.addAttribute("hata", "İşletme adı, geçerli e-posta ve telefon numarasını girin.");
            return "admin-kayit";
        }
        if (parola == null || parola.length() < 8 || parola.length() > 128) {
            model.addAttribute("hata", "Parolanız en az 8, en fazla 128 karakter olmalı.");
            return "admin-kayit";
        }
        if (!parola.equals(parolaTekrar)) {
            model.addAttribute("hata", "Parolalar aynı değil. Tekrar kontrol edin.");
            return "admin-kayit";
        }
        if (hesapRepository.existsByEposta(temizEposta)) {
            model.addAttribute("hata", "Bu e-posta adresiyle zaten bir hesap açılmış.");
            return "admin-kayit";
        }

        boolean ilkHesap = hesapRepository.count() == 0;
        boolean yoneticiHesabi = yoneticiErisimi.yoneticiEpostasiMi(temizEposta);
        if (ilkHesap && !yoneticiHesabi) {
            model.addAttribute("hata", "İlk hesap yalnızca sistem yöneticisi için tanımlı e-posta adresiyle açılabilir.");
            return "admin-kayit";
        }
        boolean onceOnaylanmis = izinliEpostaRepository.existsByEposta(temizEposta);
        IsletmeHesabi hesap = new IsletmeHesabi();
        hesap.setIsletmeAdi(temizIsletmeAdi);
        hesap.setEposta(temizEposta);
        hesap.setTelefon(temizTelefon);
        hesap.setParolaOzeti(parolaKodlayici.kodla(parola));
        boolean onayBekleyecek = !yoneticiHesabi && !ilkHesap && !onceOnaylanmis;
        hesap.setOnaylandi(!onayBekleyecek);
        hesap = hesapRepository.saveAndFlush(hesap);
        if (!ilkHesap && onceOnaylanmis) {
            izinliEpostaRepository.findByEposta(temizEposta).ifPresent(izinliEpostaRepository::delete);
        }

        if (ilkHesap) {
            IsletmeHesabi kayitliHesap = hesap;
            var eskiAraclar = aracRepository.findAllByIsletmeIsNull();
            eskiAraclar.forEach(arac -> arac.setIsletme(kayitliHesap));
            aracRepository.saveAll(eskiAraclar);
        }
        if (onayBekleyecek) {
            byte[] durumBytes = new byte[32];
            random.nextBytes(durumBytes);
            String durumKodu = Base64.getUrlEncoder().withoutPadding().encodeToString(durumBytes);
            HesapOnayTalebi talep = new HesapOnayTalebi();
            talep.setHesap(hesap);
            talep.setDurumTokenOzeti(tokenOzeti(durumKodu));
            talep.setGecerlilikZamani(Instant.now().plus(Duration.ofHours(24)));
            onayRepository.save(talep);
            try {
                onayEpostaServisi.onayIste(hesap);
            } catch (IOException | InterruptedException | RuntimeException ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                onayRepository.delete(talep);
                hesapRepository.delete(hesap);
                model.addAttribute("hata", "Onay e-postası gönderilemedi. Lütfen daha sonra tekrar deneyin.");
                return "admin-kayit";
            }
            model.addAttribute("durumKodu", durumKodu);
            model.addAttribute("eposta", temizEposta);
            return "onay-bekleniyor";
        }
        oturumAc(request, hesap);
        return "redirect:/";
    }

    @PostMapping("/giris-yap")
    public String girisYap(@RequestParam String eposta,
                           @RequestParam String parola,
                           HttpServletRequest request,
                           Model model) {
        String temizEposta = eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);
        IsletmeHesabi hesap = hesapRepository.findByEposta(temizEposta).orElse(null);
        if (hesap == null || !parolaKodlayici.dogrula(parola, hesap.getParolaOzeti())) {
            model.addAttribute("hata", "E-posta veya parola hatalı.");
            return "admin-giris";
        }
        if (!hesap.isOnaylandi()) {
            model.addAttribute("hata", "Hesabınız sistem yöneticisinin onayını bekliyor. Onay e-postası geldikten sonra giriş yapabilirsiniz.");
            return "admin-giris";
        }
        if (!hesap.isAktif() && !yoneticiErisimi.yoneticiEpostasiMi(temizEposta)) {
            model.addAttribute("hata", "Hesabınız sistem yöneticisi tarafından dondurulmuş. Hesabınızla ilgili bilgi için yöneticinize başvurun.");
            return "admin-giris";
        }
        oturumAc(request, hesap);
        return "redirect:/";
    }

    @PostMapping("/cikis")
    public String cikis(HttpSession session) {
        session.invalidate();
        return "redirect:/giris";
    }

    private void oturumAc(HttpServletRequest request, IsletmeHesabi hesap) {
        HttpSession eskiOturum = request.getSession(false);
        if (eskiOturum != null) eskiOturum.invalidate();
        HttpSession yeniOturum = request.getSession(true);
        yeniOturum.setAttribute("isletmeId", hesap.getId());
        yeniOturum.setAttribute("isletmeAdi", hesap.getIsletmeAdi());
    }

    private String tokenOzeti(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
