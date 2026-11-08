package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final AracRepository aracRepository;
    private final ServisKaydiRepository servisKaydiRepository;

    public HomeController(AracRepository aracRepository,
                          ServisKaydiRepository servisKaydiRepository) {
        this.aracRepository = aracRepository;
        this.servisKaydiRepository = servisKaydiRepository;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        Long isletmeId = (Long) session.getAttribute("isletmeId");

        long aracSayisi = aracRepository.countByIsletme_Id(isletmeId);

        var servisler = servisKaydiRepository.findAllByArac_Isletme_Id(isletmeId);

        long servisSayisi = servisler.size();

        long tamamlananIs = servisler.stream()
                .filter(s -> s.getDurum() != null)
                .filter(s -> s.getDurum().equalsIgnoreCase("Tamamlandı"))
                .count();

        double toplamGelir = servisler.stream()
                .mapToDouble(s -> s.getToplamUcret())
                .sum();

        model.addAttribute("aracSayisi", aracSayisi);
        model.addAttribute("servisSayisi", servisSayisi);
        model.addAttribute("tamamlananIs", tamamlananIs);
        model.addAttribute("toplamGelir", toplamGelir);
        model.addAttribute("isletmeAdi", session.getAttribute("isletmeAdi"));

        return "index";
    }

    @GetMapping("/araclar")
    public String araclar(Model model, HttpSession session) {
        Long isletmeId = (Long) session.getAttribute("isletmeId");
        model.addAttribute("araclar", aracRepository.findAllByIsletme_IdOrderByIdDesc(isletmeId));
        return "araclar";
    }

    @GetMapping("/servisler")
    public String servisler(Model model, HttpSession session) {
        Long isletmeId = (Long) session.getAttribute("isletmeId");
        model.addAttribute("servisKayitlari", servisKaydiRepository.findAllByArac_Isletme_Id(isletmeId));
        return "servisler";
    }

    @GetMapping("/takip/{id}")
    public String takip(@PathVariable Long id, Model model, HttpSession session) {

        Long isletmeId = (Long) session.getAttribute("isletmeId");
        Arac arac = aracRepository.findByIdAndIsletme_Id(id, isletmeId).orElse(null);

        if (arac == null) {
            return "redirect:/";
        }

        model.addAttribute("arac", arac);
        model.addAttribute("servisKayitlari",
                servisKaydiRepository.findByAracId(id));

        return "takip";
    }
}
