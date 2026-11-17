package com.autoservis;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import java.util.UUID;

@Controller
public class SistemYoneticiController {
    private final SistemYoneticiErisimi yoneticiErisimi;
    private final IsletmeHesabiRepository hesapRepository;
    private final AracRepository aracRepository;
    private final ServisKaydiRepository servisKaydiRepository;
    private final HesapOnayTalebiRepository onayTalebiRepository;
    private final SifreSifirlamaKoduRepository sifreSifirlamaKoduRepository;

    public SistemYoneticiController(SistemYoneticiErisimi yoneticiErisimi,
                                    IsletmeHesabiRepository hesapRepository,
                                    AracRepository aracRepository,
                                    ServisKaydiRepository servisKaydiRepository,
                                    HesapOnayTalebiRepository onayTalebiRepository,
                                    SifreSifirlamaKoduRepository sifreSifirlamaKoduRepository) {
        this.yoneticiErisimi = yoneticiErisimi;
        this.hesapRepository = hesapRepository;
        this.aracRepository = aracRepository;
        this.servisKaydiRepository = servisKaydiRepository;
        this.onayTalebiRepository = onayTalebiRepository;
        this.sifreSifirlamaKoduRepository = sifreSifirlamaKoduRepository;
    }

    @GetMapping("/yonetici")
    public String panel(HttpSession session, Model model, HttpServletResponse response) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";

        response.setHeader("Cache-Control", "no-store, private");
        String hesapSilmeToken = (String) session.getAttribute("hesapSilmeToken");
        if (hesapSilmeToken == null) {
            hesapSilmeToken = UUID.randomUUID().toString();
            session.setAttribute("hesapSilmeToken", hesapSilmeToken);
        }
        model.addAttribute("hesapSilmeToken", hesapSilmeToken);
        List<IsletmeHesabi> hesaplar = hesapRepository.findAllByOrderByIdDesc();
        model.addAttribute("hesaplar", hesaplar);
        model.addAttribute("bekleyenSayisi", hesaplar.stream().filter(hesap -> !hesap.isOnaylandi()).count());
        model.addAttribute("yoneticiEpostasi", session.getAttribute("isletmeId") == null
                ? "" : hesapRepository.findById((Long) session.getAttribute("isletmeId"))
                .map(IsletmeHesabi::getEposta).orElse(""));
        return "sistem-yonetici";
    }

    @PostMapping("/yonetici/hesap-sil/{id}")
    @Transactional
    public String hesapSil(@PathVariable Long id, @RequestParam String token, HttpSession session) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        String beklenenToken = (String) session.getAttribute("hesapSilmeToken");
        if (beklenenToken == null || token == null || !MessageDigest.isEqual(
                beklenenToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return "redirect:/yonetici?guvenlik";
        }
        if (!hesapRepository.existsById(id)) return "redirect:/yonetici?bulunamadi";

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
}
