package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import java.util.Map;
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
        if (talep.getHesap().isOnaylandi()) model.addAttribute("onaylandi", true);
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
        model.addAttribute("onaylandi", true);
        model.addAttribute("eposta", hesap.getEposta());
        return "hesap-onay";
    }

    @PostMapping("/hesap-onay-durumu")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> durum(@RequestParam String kod) {
        if (kod == null || kod.length() < 32 || kod.length() > 100) {
            return ResponseEntity.badRequest().body(Map.of("onaylandi", false));
        }
        HesapOnayTalebi talep = talepler.findByDurumTokenOzeti(hash(kod)).orElse(null);
        if (talep == null || talep.getGecerlilikZamani().isBefore(Instant.now())) {
            return ResponseEntity.ok(Map.of("onaylandi", false));
        }
        return ResponseEntity.ok(Map.of("onaylandi", talep.getHesap().isOnaylandi()));
    }

    private HesapOnayTalebi bul(String token) {
        if (token == null || token.length() < 32 || token.length() > 100) return null;
        return talepler.findByTokenOzeti(hash(token)).orElse(null);
    }

    private String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
