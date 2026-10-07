package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final AracRepository aracRepository;

    public HomeController(AracRepository aracRepository) {
        this.aracRepository = aracRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/araclar")
    public String araclar(Model model) {
        model.addAttribute("araclar", aracRepository.findAll());
        return "araclar";
    }

    @GetMapping("/arac-ekle")
    public String aracEkle() {
        return "arac-ekle";
    }

    @GetMapping("/arac-kaydet")
    public String aracKaydet(
            @RequestParam String plaka,
            @RequestParam String marka,
            @RequestParam String model,
            @RequestParam int yil,
            @RequestParam String telefon,
            @RequestParam String durum) {

        Arac arac = new Arac();

        arac.setPlaka(plaka);
        arac.setMarka(marka);
        arac.setModel(model);
        arac.setYil(yil);
        arac.setTelefon(telefon);
        arac.setDurum(durum);

        aracRepository.save(arac);

        return "redirect:/araclar";
    }

    @GetMapping("/takip/{id}")
    public String takip(@PathVariable Long id, Model model) {

        Arac arac = aracRepository.findById(id).orElse(null);

        model.addAttribute("arac", arac);

        return "takip";
    }
}