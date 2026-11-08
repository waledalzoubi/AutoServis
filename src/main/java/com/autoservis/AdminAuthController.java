package com.autoservis;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Controller
public class AdminAuthController {
    @Value("${autoservis.admin.password:}")
    private String adminPassword;

    @GetMapping("/giris")
    public String giris() {
        return "admin-giris";
    }

    @PostMapping("/giris-yap")
    public String girisYap(@RequestParam String sifre, HttpSession session, Model model) {
        if (adminPassword == null || adminPassword.isBlank()) {
            model.addAttribute("hata", "Tamirci parolası ayarlanmamış. AUTOSERVIS_ADMIN_PASSWORD değerini tanımlayın.");
            return "admin-giris";
        }
        boolean dogru = MessageDigest.isEqual(
                adminPassword.getBytes(StandardCharsets.UTF_8),
                sifre.getBytes(StandardCharsets.UTF_8));
        if (!dogru) {
            model.addAttribute("hata", "Parola hatalı. Tekrar deneyin.");
            return "admin-giris";
        }
        session.setAttribute("adminGirisYapti", true);
        return "redirect:/";
    }

    @PostMapping("/cikis")
    public String cikis(HttpSession session) {
        session.invalidate();
        return "redirect:/giris";
    }
}
