package com.autoservis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class SifreSifirlamaController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SifreSifirlamaController.class);
    private static final int KOD_GECERLILIK_DAKIKA = 10;
    private static final int EN_COK_HATALI_DENEME = 5;
    private static final Duration YENIDEN_GONDERME_ARALIGI = Duration.ofSeconds(60);
    private static final Pattern EMAIL_IN_ERROR = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final IsletmeHesabiRepository hesapRepository;
    private final SifreSifirlamaKoduRepository kodRepository;
    private final ParolaKodlayici parolaKodlayici;
    private final SecureRandom random = new SecureRandom();
    private final String mailFrom;
    private final String resendApiKey;
    private final ObjectMapper objectMapper;

    public SifreSifirlamaController(
            IsletmeHesabiRepository hesapRepository,
            SifreSifirlamaKoduRepository kodRepository,
            ParolaKodlayici parolaKodlayici,
            @Value("${autoservis.mail.from:}") String mailFrom,
            @Value("${autoservis.mail.resend.api-key:}") String resendApiKey,
            ObjectMapper objectMapper) {
        this.hesapRepository = hesapRepository;
        this.kodRepository = kodRepository;
        this.parolaKodlayici = parolaKodlayici;
        this.mailFrom = mailFrom;
        this.resendApiKey = resendApiKey;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/sifre-unuttum")
    public String sifreUnuttum() {
        return "sifre-unuttum";
    }

    @PostMapping("/sifre-kodu-gonder")
    public String kodGonder(@RequestParam String eposta, Model model) {
        String temizEposta = eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);
        model.addAttribute("eposta", temizEposta);

        if (!mailAyarlandiMi()) {
            model.addAttribute("hata", "E-posta gönderimi henüz yapılandırılmadı. Lütfen servis yöneticisine başvurun.");
            return "sifre-unuttum";
        }
        if (temizEposta.isBlank()) {
            model.addAttribute("hata", "E-posta adresinizi girin.");
            return "sifre-unuttum";
        }

        model.addAttribute("bilgi", "Adres kayıtlıysa sıfırlama kodu gönderildi. Kod 10 dakika geçerlidir.");

        IsletmeHesabi hesap = hesapRepository.findByEposta(temizEposta).orElse(null);
        if (hesap == null) return "sifre-unuttum";

        Instant simdi = Instant.now();
        SifreSifirlamaKodu sifirlama = kodRepository.findByHesap_Id(hesap.getId()).orElse(null);
        if (sifirlama != null && sifirlama.getOlusturulmaZamani().isAfter(simdi.minus(YENIDEN_GONDERME_ARALIGI))) {
            return "sifre-unuttum";
        }

        String kod = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        if (sifirlama == null) {
            sifirlama = new SifreSifirlamaKodu();
            sifirlama.setHesap(hesap);
        }
        sifirlama.setKodOzeti(parolaKodlayici.kodla(kod));
        sifirlama.setOlusturulmaZamani(simdi);
        sifirlama.setGecerlilikZamani(simdi.plus(Duration.ofMinutes(KOD_GECERLILIK_DAKIKA)));
        sifirlama.setHataliDenemeSayisi(0);
        sifirlama = kodRepository.save(sifirlama);

        try {
            sifirlamaEpostasiGonder(temizEposta, kod);
            model.addAttribute("kodIletildi", true);
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            try {
                kodRepository.delete(sifirlama);
            } catch (RuntimeException cleanupException) {
                LOGGER.warn("Failed to remove an unsent password reset code.");
            }
            model.addAttribute("bilgi", null);
            model.addAttribute("kodIletildi", false);
            model.addAttribute("hata", "E-posta gönderilemedi. Lütfen e-posta gönderim ayarlarını kontrol edin.");
            String hataNedeni = ex.getMessage();
            if (hataNedeni != null) hataNedeni = EMAIL_IN_ERROR.matcher(hataNedeni).replaceAll("[email]");
            LOGGER.warn("Password reset email delivery failed ({}): {}", ex.getClass().getSimpleName(), hataNedeni);
        }
        return "sifre-unuttum";
    }

    @PostMapping("/sifre-yenile")
    public String sifreYenile(
            @RequestParam String eposta,
            @RequestParam String kod,
            @RequestParam String yeniParola,
            @RequestParam String parolaTekrar,
            Model model) {
        String temizEposta = eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);
        model.addAttribute("eposta", temizEposta);
        model.addAttribute("kodIletildi", true);

        if (yeniParola == null || yeniParola.length() < 8 || yeniParola.length() > 128) {
            model.addAttribute("hata", "Yeni parola en az 8, en fazla 128 karakter olmalı.");
            return "sifre-unuttum";
        }
        if (!yeniParola.equals(parolaTekrar)) {
            model.addAttribute("hata", "Yeni parolalar aynı değil.");
            return "sifre-unuttum";
        }

        IsletmeHesabi hesap = hesapRepository.findByEposta(temizEposta).orElse(null);
        SifreSifirlamaKodu sifirlama = hesap == null ? null
                : kodRepository.findByHesap_Id(hesap.getId()).orElse(null);
        if (sifirlama == null || sifirlama.getGecerlilikZamani().isBefore(Instant.now())
                || sifirlama.getHataliDenemeSayisi() >= EN_COK_HATALI_DENEME) {
            model.addAttribute("hata", "Kod geçersiz veya süresi dolmuş. Yeni kod isteyin.");
            return "sifre-unuttum";
        }
        if (!parolaKodlayici.dogrula(kod, sifirlama.getKodOzeti())) {
            sifirlama.setHataliDenemeSayisi(sifirlama.getHataliDenemeSayisi() + 1);
            kodRepository.save(sifirlama);
            model.addAttribute("hata", "Kod geçersiz veya süresi dolmuş. Yeni kod isteyin.");
            return "sifre-unuttum";
        }

        hesap.setParolaOzeti(parolaKodlayici.kodla(yeniParola));
        hesapRepository.save(hesap);
        kodRepository.delete(sifirlama);
        return "redirect:/giris?parolaDegisti";
    }

    private boolean mailAyarlandiMi() {
        return resendApiKey != null && !resendApiKey.isBlank()
                && mailFrom != null && !mailFrom.isBlank();
    }

    private void sifirlamaEpostasiGonder(String alici, String kod) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
                "from", mailFrom,
                "to", List.of(alici),
                "subject", "AutoServis şifre sıfırlama kodu",
                "text", "AutoServis hesabınız için şifre sıfırlama kodunuz: " + kod
                        + "\n\nKod 10 dakika içinde sona erer. Bu isteği siz başlatmadıysanız bu e-postayı yok sayın."
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + resendApiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Resend API returned HTTP " + response.statusCode());
        }
    }
}
