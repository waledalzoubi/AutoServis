package com.autoservis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Locale;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
            HttpServletResponse response) {

        Arac bulunanArac = aracRepository.findByTakipAnahtari(anahtar).orElse(null);

        if (bulunanArac == null) {
            return "redirect:/musteri";
        }

        response.setHeader("Cache-Control", "no-store, private");
        // The unguessable per-vehicle key is the customer's private access link.
        // Do not require a prior VIN/phone session: WhatsApp links open directly.
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
        model.addAttribute("servisBaslangic", zamanMetni(bulunanArac.getServisBaslangicZamani()));
        model.addAttribute("servisBitis", zamanMetni(bulunanArac.getServisBitisZamani()));
        model.addAttribute("servisSuresi", servisSuresi(bulunanArac));

        return "musteri-takip";
    }

    private String zamanMetni(LocalDateTime zaman) {
        return zaman == null ? null : zaman.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private String servisSuresi(Arac arac) {
        LocalDateTime baslangic = arac.getServisBaslangicZamani();
        if (baslangic == null) return "Başlangıç zamanı kaydedilmemiş";

        LocalDateTime bitis = arac.getServisBitisZamani();
        LocalDateTime simdi = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        Duration sure = Duration.between(baslangic, bitis == null ? simdi : bitis);
        long dakika = Math.max(0, sure.toMinutes());
        long gun = dakika / 1440;
        long saat = (dakika % 1440) / 60;
        long kalanDakika = dakika % 60;
        String gecenSure = (gun > 0 ? gun + " gün " : "")
                + (saat > 0 ? saat + " saat " : "")
                + kalanDakika + " dakika";
        return bitis == null ? "Devam ediyor — " + gecenSure : gecenSure;
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
