package com.autoservis;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;
import java.util.regex.Pattern;

@Controller
public class EpostaYetkilendirmeController {
    private static final Pattern EPOSTA_BICIMI = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final IsletmeHesabiRepository hesapRepository;
    private final IzinliEpostaRepository izinliEpostaRepository;
    private final SistemYoneticiErisimi yoneticiErisimi;

    public EpostaYetkilendirmeController(IsletmeHesabiRepository hesapRepository,
                                         IzinliEpostaRepository izinliEpostaRepository,
                                         SistemYoneticiErisimi yoneticiErisimi) {
        this.hesapRepository = hesapRepository;
        this.izinliEpostaRepository = izinliEpostaRepository;
        this.yoneticiErisimi = yoneticiErisimi;
    }

    @GetMapping("/eposta-yetkilendirme")
    public String liste(HttpSession session, Model model) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        model.addAttribute("izinliEpostalar", izinliEpostaRepository.findAllByOrderByEpostaAsc());
        return "eposta-yetkilendirme";
    }

    @PostMapping("/eposta-yetkilendirme/ekle")
    public String ekle(@RequestParam String eposta, HttpSession session, Model model) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        String temizEposta = eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);
        if (temizEposta.length() > 190 || !EPOSTA_BICIMI.matcher(temizEposta).matches()) {
            model.addAttribute("hata", "Geçerli bir e-posta adresi girin.");
            model.addAttribute("izinliEpostalar", izinliEpostaRepository.findAllByOrderByEpostaAsc());
            return "eposta-yetkilendirme";
        }
        if (hesapRepository.existsByEposta(temizEposta)) {
            model.addAttribute("hata", "Bu e-posta zaten bir işletme hesabında kayıtlı.");
            model.addAttribute("izinliEpostalar", izinliEpostaRepository.findAllByOrderByEpostaAsc());
            return "eposta-yetkilendirme";
        }
        if (!izinliEpostaRepository.existsByEposta(temizEposta)) {
            IzinliEposta izin = new IzinliEposta();
            izin.setEposta(temizEposta);
            izinliEpostaRepository.save(izin);
        }
        return "redirect:/eposta-yetkilendirme?eklendi";
    }

    @PostMapping("/eposta-yetkilendirme/sil/{id}")
    public String sil(@PathVariable Long id, HttpSession session) {
        if (!yoneticiErisimi.oturumYoneticiMi(session)) return "redirect:/";
        izinliEpostaRepository.deleteById(id);
        return "redirect:/eposta-yetkilendirme?silindi";
    }
}
