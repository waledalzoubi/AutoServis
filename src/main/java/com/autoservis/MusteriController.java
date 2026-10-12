package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @RequestParam("plaka") String plaka,
            @RequestParam("telefonSon4") String telefonSon4,
            Model model) {

        String arananPlaka = normalizePlaka(plaka);
        String arananTelefon = rakamlariAl(telefonSon4);

        if (arananPlaka.isEmpty() || arananTelefon.length() != 4) {
            model.addAttribute(
                    "hata",
                    "Lütfen plakanızı ve telefonunuzun son 4 hanesini girin."
            );
            return "musteri-giris";
        }

        List<Arac> araclar = aracRepository.findAll();

        for (Arac arac : araclar) {

            if (arac.getPlaka() == null ||
                    arac.getTelefon() == null) {
                continue;
            }

            String kayitliPlaka = normalizePlaka(arac.getPlaka());
            String kayitliTelefon = rakamlariAl(arac.getTelefon());

            boolean plakaDogru =
                    kayitliPlaka.equals(arananPlaka);

            boolean telefonDogru =
                    kayitliTelefon.length() >= 4 &&
                            kayitliTelefon.endsWith(arananTelefon);

            if (plakaDogru && telefonDogru) {

                if (arac.getTakipAnahtari() == null ||
                        arac.getTakipAnahtari().isBlank()) {

                    arac.setTakipAnahtari(
                            UUID.randomUUID().toString()
                    );

                    arac = aracRepository.save(arac);
                }

                return "redirect:/musteri/takip/"
                        + arac.getTakipAnahtari();
            }
        }

        model.addAttribute(
                "hata",
                "Araç kaydı bulunamadı. Girilen bilgilerle eşleşen kayıt yok."
        );

        return "musteri-giris";
    }

    @GetMapping("/musteri/takip/{anahtar}")
    public String ozelAracTakip(
            @PathVariable("anahtar") String anahtar,
            Model model) {

        Arac bulunanArac = aracRepository.findAll()
                .stream()
                .filter(arac ->
                        arac.getTakipAnahtari() != null &&
                                arac.getTakipAnahtari().equals(anahtar))
                .findFirst()
                .orElse(null);

        if (bulunanArac == null) {
            return "redirect:/musteri";
        }

        List<ServisKaydi> kayitlar =
                servisKaydiRepository.findByAracId(
                        bulunanArac.getId()
                );

        double toplamMaliyet = kayitlar.stream()
                .mapToDouble(ServisKaydi::getToplamUcret)
                .sum();

        model.addAttribute("arac", bulunanArac);
        model.addAttribute("servisKayitlari", kayitlar);
        model.addAttribute("toplamMaliyet", toplamMaliyet);

        return "musteri-takip";
    }

    private String normalizePlaka(String plaka) {
        if (plaka == null) {
            return "";
        }

        return plaka
                .toUpperCase(new Locale("tr", "TR"))
                .replaceAll("[^A-Z0-9ÇĞİÖŞÜ]", "");
    }

    private String rakamlariAl(String metin) {
        if (metin == null) {
            return "";
        }

        return metin.replaceAll("\\D", "");
    }
}