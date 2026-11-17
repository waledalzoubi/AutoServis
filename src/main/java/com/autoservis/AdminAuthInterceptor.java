package com.autoservis;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final IsletmeHesabiRepository hesapRepository;
    private final SistemYoneticiErisimi yoneticiErisimi;

    public AdminAuthInterceptor(IsletmeHesabiRepository hesapRepository,
                                SistemYoneticiErisimi yoneticiErisimi) {
        this.hesapRepository = hesapRepository;
        this.yoneticiErisimi = yoneticiErisimi;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("isletmeId") instanceof Long hesapId) {
            IsletmeHesabi hesap = hesapRepository.findById(hesapId).orElse(null);
            if (hesap == null) {
                session.invalidate();
                response.sendRedirect("/giris");
                return false;
            }
            if (!hesap.isAktif()) {
                if (!yoneticiErisimi.oturumYoneticiMi(session)) {
                    session.invalidate();
                    response.sendRedirect("/giris?hesapDonduruldu");
                    return false;
                }
                String path = request.getRequestURI();
                if (!path.equals("/") && !path.startsWith("/yonetici")
                        && !path.startsWith("/eposta-yetkilendirme") && !path.equals("/cikis")) {
                    response.sendRedirect("/yonetici");
                    return false;
                }
            }
            return true;
        }
        response.sendRedirect("/giris");
        return false;
    }
}
