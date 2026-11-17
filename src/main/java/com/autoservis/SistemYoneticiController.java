package com.autoservis;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class SistemYoneticiController {
    private final SistemYoneticiErisimi yoneticiErisimi;
    private final IsletmeHesabiRepository hesapRepository;

    public SistemYoneticiController(SistemYoneticiErisimi yoneticiErisimi,
                                    IsletmeHesabiRepository hesapRepository) {
        this.yoneticiErisimi = yoneticiErisimi;
        this.hesapRepository = hesapRepository;
    }

    @GetMapping("/yonetici")
    public String panel(HttpSession session, Model model, HttpServletResponse response) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";

        response.setHeader("Cache-Control", "no-store, private");
        List<IsletmeHesabi> hesaplar = hesapRepository.findAllByOrderByIdDesc();
        model.addAttribute("hesaplar", hesaplar);
        model.addAttribute("bekleyenSayisi", hesaplar.stream().filter(hesap -> !hesap.isOnaylandi()).count());
        model.addAttribute("yoneticiEpostasi", session.getAttribute("isletmeId") == null
                ? "" : hesapRepository.findById((Long) session.getAttribute("isletmeId"))
                .map(IsletmeHesabi::getEposta).orElse(""));
        return "sistem-yonetici";
    }
}
