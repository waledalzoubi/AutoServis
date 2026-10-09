package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.UUID;

@Controller
public class AracController {

    private final AracRepository aracRepository;

    public AracController(AracRepository aracRepository) {
        this.aracRepository = aracRepository;
    }

    @GetMapping("/arac-ekle")
    public String aracEkle(Model model) {
        model.addAttribute("arac", new Arac());
        return "arac-ekle";
    }

    @PostMapping("/arac-kaydet")
    public String aracKaydet(@ModelAttribute Arac arac) {

        if (arac.getTakipAnahtari() == null
                || arac.getTakipAnahtari().isBlank()) {
            arac.setTakipAnahtari(UUID.randomUUID().toString());
        }

        aracRepository.save(arac);

        return "redirect:/araclar";
    }

    @GetMapping("/arac-duzenle/{id}")
    public String aracDuzenle(@PathVariable Long id, Model model) {

        Arac arac = aracRepository.findById(id).orElse(null);

        if (arac == null) {
            return "redirect:/araclar";
        }

        model.addAttribute("arac", arac);
        return "arac-duzenle";
    }

    @PostMapping("/arac-guncelle")
    public String aracGuncelle(@ModelAttribute Arac arac) {

        if (arac.getId() != null) {
            Arac mevcutArac = aracRepository
                    .findById(arac.getId())
                    .orElse(null);

            if (mevcutArac == null) {
                return "redirect:/araclar";
            }

            arac.setTakipAnahtari(mevcutArac.getTakipAnahtari());

            if (arac.getTakipAnahtari() == null
                    || arac.getTakipAnahtari().isBlank()) {
                arac.setTakipAnahtari(UUID.randomUUID().toString());
            }

            aracRepository.save(arac);
        }

        return "redirect:/araclar";
    }

    @GetMapping("/arac-sil/{id}")
    public String aracSil(@PathVariable Long id) {

        if (aracRepository.existsById(id)) {
            aracRepository.deleteById(id);
        }

        return "redirect:/araclar";
    }
}