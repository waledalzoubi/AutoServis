package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
public class MusteriController {

    private final AracRepository aracRepository;
    private final ServisKaydiRepository servisKaydiRepository;

    public MusteriController(
            AracRepository aracRepository,
            ServisKaydiRepository servisKaydiRepository) {
        this.aracRepository = aracRepository;
        this.servisKaydiRepository = servisKaydiRepository;
    }

    @GetMapping("/musteri")
    public String musteriGiris() {
        return "musteri-giris";
    }

    @PostMapping("/musteri-sorgula")
    public String musteriSorgula(
            @RequestParam String plaka,
            @RequestParam String telefonSon4,
            Model model) {

        String arananPlaka = plaka
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        String sonDortHane = telefonSon4.replaceAll("\\D", "");

        if (arananPlaka.isBlank() || sonDortHane.length() != 4) {
            model.addAttribute("hata",
                    "Plakanızı ve telefonunuzun son 4 hanesini doğru girin.");
            return "musteri-giris";
        }

        Arac bulunanArac = aracRepository.findAll()
                .stream()
                .filter(arac -> arac.getPlaka() != null
                        && arac.getPlaka().replaceAll("\\s+", "")
                        .equalsIgnoreCase(arananPlaka))
                .filter(arac -> {
                    if (arac.getTelefon() == null) {
                        return false;
                    }

                    String telefon = arac.getTelefon().replaceAll("\\D", "");
                    return telefon.length() >= 4
                            && telefon.endsWith(sonDortHane);
                })
                .findFirst()
                .orElse(null);

        if (bulunanArac == null) {
            model.addAttribute("hata",
                    "Bilgiler eşleşmedi. Plakanızı ve telefonunuzu kontrol edin.");
            return "musteri-giris";
        }

        // Eski araç kaydında anahtar yoksa oluşturup kaydet.
        if (bulunanArac.getTakipAnahtari() == null
                || bulunanArac.getTakipAnahtari().isBlank()) {
            bulunanArac.setTakipAnahtari(UUID.randomUUID().toString());
            bulunanArac = aracRepository.save(bulunanArac);
        }

        System.out.println("BULUNAN ARAÇ: " + bulunanArac.getPlaka()
                + " | TAKİP ANAHTARI: " + bulunanArac.getTakipAnahtari());

        return "redirect:/musteri/takip/"
                + bulunanArac.getTakipAnahtari();
    }

    @GetMapping("/musteri/takip/{anahtar}")
    public String ozelAracTakip(
            @PathVariable String anahtar,
            Model model) {

        Arac bulunanArac = aracRepository.findAll()
                .stream()
                .filter(arac -> arac.getTakipAnahtari() != null
                        && arac.getTakipAnahtari().equals(anahtar))
                .findFirst()
                .orElse(null);

        if (bulunanArac == null) {
            return "redirect:/musteri";
        }

        List<ServisKaydi> kayitlar =
                servisKaydiRepository.findByAracId(bulunanArac.getId());

        double toplamMaliyet = kayitlar.stream()
                .mapToDouble(ServisKaydi::getToplamUcret)
                .sum();

        model.addAttribute("arac", bulunanArac);
        model.addAttribute("servisKayitlari", kayitlar);
        model.addAttribute("toplamMaliyet", toplamMaliyet);

        return "musteri-takip";
    }
}