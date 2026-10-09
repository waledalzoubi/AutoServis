package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class ServisKaydiController {

    private final ServisKaydiRepository servisKaydiRepository;
    private final AracRepository aracRepository;

    public ServisKaydiController(
            ServisKaydiRepository servisKaydiRepository,
            AracRepository aracRepository) {
        this.servisKaydiRepository = servisKaydiRepository;
        this.aracRepository = aracRepository;
    }

    @GetMapping("/servis-ekle")
    public String servisEkle(Model model) {
        model.addAttribute("araclar", aracRepository.findAll());
        model.addAttribute("servisKaydi", new ServisKaydi());
        return "servis-ekle";
    }

    @PostMapping("/servis-kaydet")
    public String servisKaydet(
            @ModelAttribute ServisKaydi servisKaydi,
            @RequestParam("aracId") Long aracId) {

        Arac arac = aracRepository.findById(aracId).orElse(null);

        if (arac == null) {
            return "redirect:/servis-ekle";
        }

        servisKaydi.setArac(arac);
        servisKaydiRepository.save(servisKaydi);

        return "redirect:/araclar";
    }

    @GetMapping("/servis-duzenle/{id}")
    public String servisDuzenle(@org.springframework.web.bind.annotation.PathVariable Long id,
                                Model model) {
        ServisKaydi kayit = servisKaydiRepository.findById(id).orElse(null);

        if (kayit == null) {
            return "redirect:/araclar";
        }

        model.addAttribute("servisKaydi", kayit);
        model.addAttribute("araclar", aracRepository.findAll());

        return "servis-duzenle";
    }

    @PostMapping("/servis-guncelle")
    public String servisGuncelle(
            @RequestParam Long id,
            @RequestParam Long aracId,
            @RequestParam String islem,
            @RequestParam(required = false) String aciklama,
            @RequestParam java.time.LocalDate tarih,
            @RequestParam double iscilikUcreti,
            @RequestParam double parcaUcreti,
            @RequestParam String durum) {

        ServisKaydi kayit = servisKaydiRepository.findById(id).orElse(null);
        Arac arac = aracRepository.findById(aracId).orElse(null);

        if (kayit == null || arac == null) {
            return "redirect:/araclar";
        }

        kayit.setArac(arac);
        kayit.setIslem(islem);
        kayit.setAciklama(aciklama);
        kayit.setTarih(tarih);
        kayit.setIscilikUcreti(iscilikUcreti);
        kayit.setParcaUcreti(parcaUcreti);
        kayit.setDurum(durum);

        servisKaydiRepository.save(kayit);

        return "redirect:/takip/" + arac.getId();
    }

    @GetMapping("/servis-sil/{id}")
    public String servisSil(@org.springframework.web.bind.annotation.PathVariable Long id) {
        ServisKaydi kayit = servisKaydiRepository.findById(id).orElse(null);

        if (kayit == null) {
            return "redirect:/araclar";
        }

        Long aracId = kayit.getArac().getId();
        servisKaydiRepository.delete(kayit);

        return "redirect:/takip/" + aracId;
    }
}