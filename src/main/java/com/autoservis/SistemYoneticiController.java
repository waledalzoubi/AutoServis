package com.autoservis;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Controller
public class SistemYoneticiController {
    private static final Duration YONETICI_KOD_SURESI = Duration.ofMinutes(10);
    private static final Duration KOD_TEKRAR_GONDERME_ARALIGI = Duration.ofSeconds(60);
    private static final int EN_COK_HATALI_KOD = 5;

    private final SistemYoneticiErisimi yoneticiErisimi;
    private final IsletmeHesabiRepository hesapRepository;
    private final AracRepository aracRepository;
    private final ServisKaydiRepository servisKaydiRepository;
    private final HesapOnayTalebiRepository onayTalebiRepository;
    private final SifreSifirlamaKoduRepository sifreSifirlamaKoduRepository;
    private final HesapOnayEpostaServisi onayEpostaServisi;
    private final ParolaKodlayici parolaKodlayici;
    private final IzinliEpostaRepository izinliEpostaRepository;
    private final SecureRandom random = new SecureRandom();
    private final String applicationBaseUrl;

    public SistemYoneticiController(SistemYoneticiErisimi yoneticiErisimi,
                                    IsletmeHesabiRepository hesapRepository,
                                    AracRepository aracRepository,
                                    ServisKaydiRepository servisKaydiRepository,
                                    HesapOnayTalebiRepository onayTalebiRepository,
                                    SifreSifirlamaKoduRepository sifreSifirlamaKoduRepository,
                                    HesapOnayEpostaServisi onayEpostaServisi,
                                    ParolaKodlayici parolaKodlayici,
                                    IzinliEpostaRepository izinliEpostaRepository,
                                    @Value("${autoservis.base-url:https://autoservis-2f51.onrender.com}") String applicationBaseUrl) {
        this.yoneticiErisimi = yoneticiErisimi;
        this.hesapRepository = hesapRepository;
        this.aracRepository = aracRepository;
        this.servisKaydiRepository = servisKaydiRepository;
        this.onayTalebiRepository = onayTalebiRepository;
        this.sifreSifirlamaKoduRepository = sifreSifirlamaKoduRepository;
        this.onayEpostaServisi = onayEpostaServisi;
        this.parolaKodlayici = parolaKodlayici;
        this.izinliEpostaRepository = izinliEpostaRepository;
        this.applicationBaseUrl = applicationBaseUrl;
    }

    @GetMapping("/yonetici")
    public String panel(HttpSession session, Model model, HttpServletResponse response,
                        @RequestParam(required = false) Long secili) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";

        response.setHeader("Cache-Control", "no-store, private");
        String hesapSilmeToken = (String) session.getAttribute("hesapSilmeToken");
        if (hesapSilmeToken == null) {
            hesapSilmeToken = UUID.randomUUID().toString();
            session.setAttribute("hesapSilmeToken", hesapSilmeToken);
        }
        model.addAttribute("hesapSilmeToken", hesapSilmeToken);
        List<IsletmeHesabi> hesaplar = hesapRepository.findAllByOrderByIdDesc().stream()
                .filter(hesap -> !yoneticiErisimi.yoneticiEpostasiMi(hesap.getEposta()))
                .toList();
        model.addAttribute("hesaplar", hesaplar);
        model.addAttribute("seciliHesapId", secili);
        model.addAttribute("bekleyenSayisi", hesaplar.stream().filter(hesap -> !hesap.isOnaylandi()).count());
        model.addAttribute("dondurulmusSayisi", hesaplar.stream().filter(hesap -> !hesap.isAktif()).count());
        model.addAttribute("yoneticiEpostasi", session.getAttribute("isletmeId") == null
                ? "" : hesapRepository.findById((Long) session.getAttribute("isletmeId"))
                .map(IsletmeHesabi::getEposta).orElse(""));
        model.addAttribute("applicationBaseUrl", applicationBaseUrl);
        return "sistem-yonetici";
    }

    @PostMapping("/yonetici/hesap-dondur/{id}")
    public String hesapDondur(@PathVariable Long id, @RequestParam String token, HttpSession session) {
        return hesapAktifliginiDegistir(id, token, false, session);
    }

    @PostMapping("/yonetici/hesap-etkinlestir/{id}")
    public String hesapEtkinlestir(@PathVariable Long id, @RequestParam String token, HttpSession session) {
        return hesapAktifliginiDegistir(id, token, true, session);
    }

    @PostMapping("/yonetici/hesap-onay-kodu/{id}")
    public String onayKoduGonder(@PathVariable Long id, @RequestParam String token, HttpSession session) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        if (!yoneticiIslemTokeniGecerli(token, session)) return "redirect:/yonetici?guvenlik";
        IsletmeHesabi hesap = hesapRepository.findById(id).orElse(null);
        if (hesap == null) return "redirect:/yonetici?bulunamadi";
        if (yoneticiErisimi.yoneticiEpostasiMi(hesap.getEposta())) {
            return "redirect:/yonetici?yoneticiKorunuyor";
        }
        if (hesap.isOnaylandi()) return "redirect:/yonetici?zatenOnayli&secili=" + id;

        Instant simdi = Instant.now();
        HesapOnayTalebi talep = onayTalebiRepository.findByHesap_Id(id).orElseGet(() -> {
            HesapOnayTalebi yeniTalep = new HesapOnayTalebi();
            yeniTalep.setHesap(hesap);
            yeniTalep.setGecerlilikZamani(simdi.plus(Duration.ofHours(24)));
            return yeniTalep;
        });
        if (talep.getYoneticiKodGonderilmeZamani() != null
                && talep.getYoneticiKodGonderilmeZamani().isAfter(simdi.minus(KOD_TEKRAR_GONDERME_ARALIGI))) {
            return "redirect:/yonetici?kodBekle&secili=" + id;
        }

        String kod = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1_000_000));
        talep.setYoneticiKodOzeti(parolaKodlayici.kodla(kod));
        talep.setYoneticiKodSonKullanma(simdi.plus(YONETICI_KOD_SURESI));
        talep.setYoneticiKodGonderilmeZamani(simdi);
        talep.setYoneticiKodYanlisDenemeSayisi(0);
        talep.setGecerlilikZamani(simdi.plus(Duration.ofHours(24)));
        talep.setTokenOzeti(null);
        onayTalebiRepository.save(talep);
        try {
            onayEpostaServisi.yoneticiOnayKoduGonder(hesap, kod);
        } catch (java.io.IOException | InterruptedException | RuntimeException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            talep.setYoneticiKodOzeti(null);
            talep.setYoneticiKodSonKullanma(null);
            talep.setYoneticiKodGonderilmeZamani(null);
            onayTalebiRepository.save(talep);
            return "redirect:/yonetici?mailHata&secili=" + id;
        }
        return "redirect:/yonetici?kodGonderildi&secili=" + id;
    }

    @PostMapping("/yonetici/hesap-onayla/{id}")
    @Transactional
    public String hesapOnayla(@PathVariable Long id, @RequestParam String token,
                              @RequestParam String kod, HttpSession session) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        if (!yoneticiIslemTokeniGecerli(token, session)) return "redirect:/yonetici?guvenlik";
        IsletmeHesabi hesap = hesapRepository.findById(id).orElse(null);
        if (hesap == null) return "redirect:/yonetici?bulunamadi";
        if (hesap.isOnaylandi()) return "redirect:/yonetici?zatenOnayli&secili=" + id;
        HesapOnayTalebi talep = onayTalebiRepository.findByHesap_Id(id).orElse(null);
        Instant simdi = Instant.now();
        if (talep == null || talep.getYoneticiKodOzeti() == null
                || talep.getYoneticiKodSonKullanma() == null
                || talep.getYoneticiKodSonKullanma().isBefore(simdi)
                || talep.getYoneticiKodYanlisDenemeSayisi() >= EN_COK_HATALI_KOD) {
            return "redirect:/yonetici?kodSuresiDoldu&secili=" + id;
        }
        if (kod == null || !kod.matches("[0-9]{6}")
                || !parolaKodlayici.dogrula(kod, talep.getYoneticiKodOzeti())) {
            talep.setYoneticiKodYanlisDenemeSayisi(talep.getYoneticiKodYanlisDenemeSayisi() + 1);
            onayTalebiRepository.save(talep);
            return "redirect:/yonetici?kodYanlis&secili=" + id;
        }

        hesap.setOnaylandi(true);
        hesapRepository.save(hesap);
        talep.setYoneticiKodOzeti(null);
        talep.setYoneticiKodSonKullanma(null);
        talep.setYoneticiKodYanlisDenemeSayisi(0);
        onayTalebiRepository.save(talep);
        if (!izinliEpostaRepository.existsByEposta(hesap.getEposta())) {
            IzinliEposta izin = new IzinliEposta();
            izin.setEposta(hesap.getEposta());
            izinliEpostaRepository.save(izin);
        }
        return "redirect:/yonetici?onaylandi&secili=" + id;
    }

    private String hesapAktifliginiDegistir(Long id, String token, boolean aktif, HttpSession session) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        if (!yoneticiIslemTokeniGecerli(token, session)) return "redirect:/yonetici?guvenlik";
        IsletmeHesabi hesap = hesapRepository.findById(id).orElse(null);
        if (hesap == null) return "redirect:/yonetici?bulunamadi";
        if (yoneticiErisimi.yoneticiEpostasiMi(hesap.getEposta())) {
            return "redirect:/yonetici?yoneticiKorunuyor";
        }
        hesap.setAktif(aktif);
        hesapRepository.save(hesap);
        return aktif ? "redirect:/yonetici?etkinlestirildi" : "redirect:/yonetici?donduruldu";
    }

    @PostMapping("/yonetici/hesap-sil/{id}")
    @Transactional
    public String hesapSil(@PathVariable Long id, @RequestParam String token, HttpSession session) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        if (!yoneticiIslemTokeniGecerli(token, session)) return "redirect:/yonetici?guvenlik";
        IsletmeHesabi silinecekHesap = hesapRepository.findById(id).orElse(null);
        if (silinecekHesap == null) return "redirect:/yonetici?bulunamadi";
        if (yoneticiErisimi.yoneticiEpostasiMi(silinecekHesap.getEposta())) {
            return "redirect:/yonetici?yoneticiKorunuyor";
        }

        var servisKayitlari = servisKaydiRepository.findAllByArac_Isletme_Id(id);
        servisKaydiRepository.deleteAll(servisKayitlari);
        var araclar = aracRepository.findAllByIsletme_IdOrderByIdDesc(id);
        aracRepository.deleteAll(araclar);
        onayTalebiRepository.deleteByHesap_Id(id);
        sifreSifirlamaKoduRepository.deleteByHesap_Id(id);
        hesapRepository.deleteById(id);

        Object aktifHesapId = session.getAttribute("isletmeId");
        if (aktifHesapId instanceof Long && ((Long) aktifHesapId).equals(id)) {
            session.invalidate();
            return "redirect:/giris?hesapSilindi";
        }
        return "redirect:/yonetici?silindi";
    }

    private boolean yoneticiIslemTokeniGecerli(String token, HttpSession session) {
        String beklenenToken = (String) session.getAttribute("hesapSilmeToken");
        return beklenenToken != null && token != null && MessageDigest.isEqual(
                beklenenToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
}
