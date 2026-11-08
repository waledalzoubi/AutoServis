package com.autoservis;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class ServisKaydiController {
    private final ServisKaydiRepository servisKaydiRepository;
    private final AracRepository aracRepository;

    public ServisKaydiController(ServisKaydiRepository servisKaydiRepository, AracRepository aracRepository) {
        this.servisKaydiRepository = servisKaydiRepository;
        this.aracRepository = aracRepository;
    }

    @GetMapping("/servis-ekle")
    public String servisEkle(Model model, HttpSession session) {
        model.addAttribute("araclar", aracRepository.findAllByIsletme_IdOrderByIdDesc(
                (Long) session.getAttribute("isletmeId")));
        model.addAttribute("servisKaydi", new ServisKaydi());
        return "servis-ekle";
    }

    @PostMapping("/servis-kaydet")
    public String servisKaydet(@ModelAttribute ServisKaydi servisKaydi,
                               @RequestParam("aracId") Long aracId,
                               HttpSession session) {
        Arac arac = aracRepository.findByIdAndIsletme_Id(aracId,
                (Long) session.getAttribute("isletmeId")).orElse(null);
        if (arac == null) return "redirect:/servis-ekle";

        servisKaydi.setArac(arac);
        servisKaydiRepository.save(servisKaydi);
        return "redirect:/araclar";
    }

    @GetMapping("/servis-duzenle/{id}")
    public String servisDuzenle(@PathVariable Long id, Model model, HttpSession session) {
        Long isletmeId = (Long) session.getAttribute("isletmeId");
        ServisKaydi kayit = servisKaydiRepository.findByIdAndArac_Isletme_Id(id, isletmeId).orElse(null);
        if (kayit == null) return "redirect:/servisler";

        model.addAttribute("servisKaydi", kayit);
        model.addAttribute("araclar", aracRepository.findAllByIsletme_IdOrderByIdDesc(isletmeId));
        return "servis-duzenle";
    }

    @PostMapping("/servis-guncelle")
    public String servisGuncelle(@RequestParam Long id,
                                @RequestParam Long aracId,
                                @RequestParam String islem,
                                @RequestParam(required = false) String aciklama,
                                @RequestParam LocalDate tarih,
                                @RequestParam double iscilikUcreti,
                                @RequestParam double parcaUcreti,
                                @RequestParam String durum,
                                HttpSession session) {
        Long isletmeId = (Long) session.getAttribute("isletmeId");
        ServisKaydi kayit = servisKaydiRepository.findByIdAndArac_Isletme_Id(id, isletmeId).orElse(null);
        Arac arac = aracRepository.findByIdAndIsletme_Id(aracId, isletmeId).orElse(null);
        if (kayit == null || arac == null) return "redirect:/servisler";

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
    public String servisSil(@PathVariable Long id, HttpSession session) {
        ServisKaydi kayit = servisKaydiRepository.findByIdAndArac_Isletme_Id(id,
                (Long) session.getAttribute("isletmeId")).orElse(null);
        if (kayit == null) return "redirect:/servisler";

        Long aracId = kayit.getArac().getId();
        servisKaydiRepository.delete(kayit);
        return "redirect:/takip/" + aracId;
    }
}
