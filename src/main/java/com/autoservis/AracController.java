package com.autoservis;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AracController {
    private final AracRepository aracRepository;
    private final IsletmeHesabiRepository isletmeHesabiRepository;

    public AracController(AracRepository aracRepository, IsletmeHesabiRepository isletmeHesabiRepository) {
        this.aracRepository = aracRepository;
        this.isletmeHesabiRepository = isletmeHesabiRepository;
    }

    @GetMapping("/arac-ekle")
    public String aracEkle(Model model) {
        model.addAttribute("arac", new Arac());
        return "arac-ekle";
    }

    @PostMapping("/arac-kaydet")
    public String aracKaydet(@ModelAttribute Arac arac, HttpSession session) {
        Long isletmeId = (Long) session.getAttribute("isletmeId");
        IsletmeHesabi isletme = isletmeHesabiRepository.findById(isletmeId).orElse(null);
        if (isletme == null) return "redirect:/giris";

        arac.setIsletme(isletme);
        aracRepository.save(arac);
        return "redirect:/araclar";
    }

    @GetMapping("/arac-duzenle/{id}")
    public String aracDuzenle(@PathVariable Long id, Model model, HttpSession session) {
        Arac arac = aracRepository.findByIdAndIsletme_Id(id, (Long) session.getAttribute("isletmeId"))
                .orElse(null);
        if (arac == null) return "redirect:/araclar";
        model.addAttribute("arac", arac);
        return "arac-duzenle";
    }

    @PostMapping("/arac-guncelle")
    public String aracGuncelle(
            @RequestParam Long id,
            @RequestParam String plaka,
            @RequestParam String marka,
            @RequestParam String model,
            @RequestParam int yil,
            @RequestParam String telefon,
            @RequestParam String sasiNumarasi,
            @RequestParam String durum,
            HttpSession session) {
        Arac mevcut = aracRepository.findByIdAndIsletme_Id(id, (Long) session.getAttribute("isletmeId"))
                .orElse(null);
        if (mevcut == null) return "redirect:/araclar";

        mevcut.setPlaka(plaka);
        mevcut.setMarka(marka);
        mevcut.setModel(model);
        mevcut.setYil(yil);
        mevcut.setTelefon(telefon);
        mevcut.setSasiNumarasi(sasiNumarasi);
        mevcut.setDurum(durum);
        aracRepository.save(mevcut);
        return "redirect:/araclar";
    }

    @GetMapping("/arac-sil/{id}")
    public String aracSil(@PathVariable Long id, HttpSession session) {
        aracRepository.findByIdAndIsletme_Id(id, (Long) session.getAttribute("isletmeId"))
                .ifPresent(aracRepository::delete);
        return "redirect:/araclar";
    }
}
