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

    private final IsletmeHesabiRepository hesapRepository;
    private final AracRepository aracRepository;
    private final ParolaKodlayici parolaKodlayici;
    private final IzinliEpostaRepository izinliEpostaRepository;
    private final SistemYoneticiErisimi yoneticiErisimi;
    private final HesapOnayTalebiRepository onayRepository;
    private final HesapOnayEpostaServisi onayEpostaServisi;
    private final SecureRandom random = new SecureRandom();

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

    @PostMapping("/kayit-yap")
    @Transactional
    public String kayitYap(@RequestParam String isletmeAdi,
                           @RequestParam String eposta,
                           @RequestParam String parola,
                           @RequestParam String parolaTekrar,
                           HttpServletRequest request,
                           Model model) {
        String temizIsletmeAdi = isletmeAdi == null ? "" : isletmeAdi.trim();
        String temizEposta = eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);

        if (temizIsletmeAdi.isBlank() || temizIsletmeAdi.length() > 120
                || !EPOSTA_BICIMI.matcher(temizEposta).matches()) {
            model.addAttribute("hata", "İşletme adını ve geçerli bir e-posta adresini girin.");
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
        if (ilkHesap && !yoneticiErisimi.yoneticiEpostasiMi(temizEposta)) {
            model.addAttribute("hata", "İlk hesap yalnızca sistem yöneticisi için tanımlı e-posta adresiyle açılabilir.");
            return "admin-kayit";
        }
        boolean onceOnaylanmis = izinliEpostaRepository.existsByEposta(temizEposta);
        IsletmeHesabi hesap = new IsletmeHesabi();
        hesap.setIsletmeAdi(temizIsletmeAdi);
        hesap.setEposta(temizEposta);
        hesap.setParolaOzeti(parolaKodlayici.kodla(parola));
        boolean onayBekleyecek = !ilkHesap && !onceOnaylanmis;
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
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            HesapOnayTalebi talep = new HesapOnayTalebi();
            talep.setHesap(hesap);
            talep.setTokenOzeti(tokenOzeti(token));
            talep.setGecerlilikZamani(Instant.now().plus(Duration.ofHours(24)));
            onayRepository.save(talep);
            try {
                onayEpostaServisi.onayIste(hesap, token);
            } catch (IOException | InterruptedException | RuntimeException ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                onayRepository.delete(talep);
                hesapRepository.delete(hesap);
                model.addAttribute("hata", "Onay e-postası gönderilemedi. Lütfen daha sonra tekrar deneyin.");
                return "admin-kayit";
            }
            model.addAttribute("epostaOnayiBekleniyor", true);
            model.addAttribute("basarili", "Kayıt isteğiniz size özel incelemeye gönderildi. Onay e-postası geldikten sonra giriş yapabilirsiniz.");
            return "admin-giris";
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
