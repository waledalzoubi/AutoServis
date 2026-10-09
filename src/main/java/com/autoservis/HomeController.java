package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    public String home(Model model) {

        long aracSayisi = aracRepository.count();

        var servisler = servisKaydiRepository.findAll();

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

        return "index";
    }

    @GetMapping("/araclar")
    public String araclar(Model model) {
        model.addAttribute("araclar", aracRepository.findAll());
        return "araclar";
    }

    @GetMapping("/takip/{id}")
    public String takip(@PathVariable Long id, Model model) {

        Arac arac = aracRepository.findById(id).orElse(null);

        if (arac == null) {
            return "redirect:/";
        }

        model.addAttribute("arac", arac);
        model.addAttribute("servisKayitlari",
                servisKaydiRepository.findByAracId(id));

        return "takip";
    }
}