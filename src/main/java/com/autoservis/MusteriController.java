package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Locale;

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
            @RequestParam("sasiNumarasi") String sasiNumarasi,
            @RequestParam("telefon") String telefon,
            Model model,
            HttpSession session) {
        session.removeAttribute("musteriAracId");
        String arananSasi = normalizeSasi(sasiNumarasi);
        String arananTelefon = normalizeTelefon(telefon);

        if (arananSasi.isBlank() || arananTelefon.length() < 10) {
            model.addAttribute("hata", "Şasi numaranızı ve kayıtlı telefon numaranızı girin.");
            return "musteri-giris";
        }

        Arac arac = aracRepository.findBySasiNumarasi(arananSasi).orElse(null);
        if (arac != null && arac.getTelefon() != null
                && normalizeTelefon(arac.getTelefon()).equals(arananTelefon)) {
            session.setAttribute("musteriAracId", arac.getId());
            return musteriAraciniGoster(arac, model);
        }

        model.addAttribute("hata", "Bilgiler eşleşmedi. Şasi ve telefon numaranızı kontrol edin.");
        return "musteri-giris";
    }

    @GetMapping("/musteri/takip/{anahtar}")
    public String ozelAracTakip(
            @PathVariable("anahtar") String anahtar,
            Model model,
            HttpSession session) {

        Arac bulunanArac = aracRepository.findAll()
                .stream()
                .filter(arac -> arac.getTakipAnahtari() != null
                        && arac.getTakipAnahtari().equals(anahtar))
                .findFirst()
                .orElse(null);

        if (bulunanArac == null) {
            return "redirect:/musteri";
        }

        Object izinliAracId = session.getAttribute("musteriAracId");
        if (izinliAracId == null || !izinliAracId.equals(bulunanArac.getId())) {
            return "redirect:/musteri";
        }

        return musteriAraciniGoster(bulunanArac, model);
    }

    private String musteriAraciniGoster(Arac bulunanArac, Model model) {
        List<String> durumlar = List.of(
                "Araç servise alındı",
                "Arıza tespiti yapılıyor",
                "Parça bekleniyor",
                "Onarım yapılıyor",
                "Test aşamasında",
                "Araç hazır",
                "Teslim edildi");
        List<ServisKaydi> kayitlar =
                servisKaydiRepository.findByAracId(bulunanArac.getId());

        double toplamMaliyet = kayitlar.stream()
                .mapToDouble(ServisKaydi::getToplamUcret)
                .sum();

        model.addAttribute("arac", bulunanArac);
        model.addAttribute("servisKayitlari", kayitlar);
        model.addAttribute("toplamMaliyet", toplamMaliyet);
        model.addAttribute("durumlar", durumlar);
        model.addAttribute("durumSirasi", durumlar.indexOf(bulunanArac.getDurum()));

        return "musteri-takip";
    }

    private String normalizeSasi(String sasi) {
        return sasi == null ? "" : sasi.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeTelefon(String telefon) {
        if (telefon == null) {
            return "";
        }
        String rakamlar = telefon.replaceAll("\\D", "");
        if (rakamlar.startsWith("0090")) rakamlar = rakamlar.substring(4);
        else if (rakamlar.startsWith("90") && rakamlar.length() == 12) rakamlar = rakamlar.substring(2);
        if (rakamlar.length() == 10 && rakamlar.startsWith("5")) rakamlar = "0" + rakamlar;
        return rakamlar;
    }
}
