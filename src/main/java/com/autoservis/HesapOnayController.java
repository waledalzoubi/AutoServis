package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Controller
public class HesapOnayController {
    private final HesapOnayTalebiRepository talepler;
    private final IzinliEpostaRepository izinliEpostalar;
    private final IsletmeHesabiRepository hesaplar;

    public HesapOnayController(HesapOnayTalebiRepository talepler,
                               IzinliEpostaRepository izinliEpostalar,
                               IsletmeHesabiRepository hesaplar) {
        this.talepler = talepler;
        this.izinliEpostalar = izinliEpostalar;
        this.hesaplar = hesaplar;
    }

    @GetMapping("/hesap-onay")
    public String sayfa(@RequestParam(required = false) String token, Model model) {
        HesapOnayTalebi talep = bul(token);
        if (talep == null || talep.getGecerlilikZamani().isBefore(Instant.now())) {
            model.addAttribute("hata", "Onay bağlantısı geçersiz veya süresi dolmuş.");
            return "hesap-onay";
        }
        model.addAttribute("token", token);
        model.addAttribute("isletmeAdi", talep.getHesap().getIsletmeAdi());
        model.addAttribute("eposta", talep.getHesap().getEposta());
        return "hesap-onay";
    }

    @PostMapping("/hesap-onay")
    @Transactional
    public String onayla(@RequestParam String token, Model model) {
        HesapOnayTalebi talep = bul(token);
        if (talep == null || talep.getGecerlilikZamani().isBefore(Instant.now())) {
            model.addAttribute("hata", "Onay bağlantısı geçersiz veya süresi dolmuş.");
            return "hesap-onay";
        }
        IsletmeHesabi hesap = talep.getHesap();
        hesap.setOnaylandi(true);
        hesaplar.save(hesap);
        if (!izinliEpostalar.existsByEposta(hesap.getEposta())) {
            IzinliEposta izin = new IzinliEposta();
            izin.setEposta(hesap.getEposta());
            izinliEpostalar.save(izin);
        }
        talepler.delete(talep);
        model.addAttribute("onaylandi", true);
        model.addAttribute("eposta", hesap.getEposta());
        return "hesap-onay";
    }

    private HesapOnayTalebi bul(String token) {
        if (token == null || token.length() < 32 || token.length() > 100) return null;
        try {
            String hash = java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
            return talepler.findByTokenOzeti(hash).orElse(null);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
