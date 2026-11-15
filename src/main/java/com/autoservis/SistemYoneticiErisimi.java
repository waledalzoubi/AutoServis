package com.autoservis;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SistemYoneticiErisimi {
    private final IsletmeHesabiRepository hesapRepository;
    private final String yoneticiEpostasi;

    public SistemYoneticiErisimi(IsletmeHesabiRepository hesapRepository,
                                 @Value("${autoservis.admin.email:}") String yoneticiEpostasi) {
        this.hesapRepository = hesapRepository;
        this.yoneticiEpostasi = normalize(yoneticiEpostasi);
    }

    public boolean oturumYoneticiMi(HttpSession session) {
        if (yoneticiEpostasi.isBlank()) return false;
        Long hesapId = (Long) session.getAttribute("isletmeId");
        if (hesapId == null) return false;
        return hesapRepository.findById(hesapId)
                .map(IsletmeHesabi::getEposta)
                .map(SistemYoneticiErisimi::normalize)
                .filter(yoneticiEpostasi::equals)
                .isPresent();
    }

    public boolean yoneticiEpostasiMi(String eposta) {
        return !yoneticiEpostasi.isBlank() && yoneticiEpostasi.equals(normalize(eposta));
    }

    private static String normalize(String eposta) {
        return eposta == null ? "" : eposta.trim().toLowerCase(Locale.ROOT);
    }
}
