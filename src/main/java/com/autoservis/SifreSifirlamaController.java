package com.autoservis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
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

    private final IsletmeHesabiRepository hesapRepository;
    private final SifreSifirlamaKoduRepository kodRepository;
    private final ParolaKodlayici parolaKodlayici;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final String mailFrom;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;
    private final boolean smtpAuth;

    public SifreSifirlamaController(
            IsletmeHesabiRepository hesapRepository,
            SifreSifirlamaKoduRepository kodRepository,
            ParolaKodlayici parolaKodlayici,
            JavaMailSender mailSender,
            @Value("${autoservis.mail.from:}") String mailFrom,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth) {
        this.hesapRepository = hesapRepository;
        this.kodRepository = kodRepository;
        this.parolaKodlayici = parolaKodlayici;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.smtpAuth = smtpAuth;
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

        SimpleMailMessage mesaj = new SimpleMailMessage();
        mesaj.setFrom(mailFrom == null ? "" : mailFrom.replaceAll("\\s+", ""));
        mesaj.setTo(temizEposta);
        mesaj.setSubject("AutoServis şifre sıfırlama kodu");
        mesaj.setText("AutoServis hesabınız için şifre sıfırlama kodunuz: " + kod
                + "\n\nKod 10 dakika içinde sona erer. Bu isteği siz başlatmadıysanız bu e-postayı yok sayın.");
        try {
            if (mailSender instanceof JavaMailSenderImpl sender) {
                sender.setUsername(mailUsername == null ? "" : mailUsername.replaceAll("\\s+", ""));
                sender.setPassword(mailPassword == null ? "" : mailPassword.replaceAll("\\s+", ""));
            }
            mailSender.send(mesaj);
            model.addAttribute("kodIletildi", true);
        } catch (MailException ex) {
            try {
                kodRepository.delete(sifirlama);
            } catch (RuntimeException cleanupException) {
                LOGGER.warn("Failed to remove an unsent password reset code.");
            }
            model.addAttribute("bilgi", null);
            model.addAttribute("kodIletildi", false);
            model.addAttribute("hata", "E-posta gönderilemedi. Lütfen e-posta gönderim ayarlarını kontrol edin.");
            String hataNedeni = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
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
        model.addAttribute("kodIletildi", false);
        model.addAttribute("eposta", "");
        model.addAttribute("basarili", "Parolanız değiştirildi. Yeni parolanızla giriş yapabilirsiniz.");
        return "sifre-unuttum";
    }

    private boolean mailAyarlandiMi() {
        return mailHost != null && !mailHost.isBlank()
                && mailFrom != null && !mailFrom.isBlank()
                && (!smtpAuth || (mailUsername != null && !mailUsername.isBlank()
                    && mailPassword != null && !mailPassword.isBlank()));
    }
}
