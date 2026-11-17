package com.autoservis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class HesapOnayEpostaServisi {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;
    private final String from;
    private final String adminEmail;
    private final ObjectMapper mapper;

    public HesapOnayEpostaServisi(
            @Value("${autoservis.mail.resend.api-key:}") String apiKey,
            @Value("${autoservis.mail.from:}") String from,
            @Value("${autoservis.admin.email:}") String adminEmail,
            ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.from = from;
        this.adminEmail = adminEmail;
        this.mapper = mapper;
    }

    public void onayIste(IsletmeHesabi hesap) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()
                || adminEmail == null || adminEmail.isBlank()) {
            throw new IOException("Email settings are not configured");
        }
        String bodyText = "Yeni AutoServis işletme hesabı kayıt isteği\n\n"
                + "İşletme: " + hesap.getIsletmeAdi() + "\nE-posta: " + hesap.getEposta()
                + "\nTelefon: " + (hesap.getTelefon() == null ? "Belirtilmemiş" : hesap.getTelefon())
                + "\n\nHesabı onaylamak için yönetici panelinde işletme adını açın, “Onayla · kod gönder” seçeneğini kullanın ve yönetici e-postanıza gelen 6 haneli kodu girin."
                + "\n\nBu isteği tanımıyorsanız yok sayabilirsiniz.";
        Map<String, Object> body = Map.of("from", from, "to", List.of(adminEmail),
                "subject", "AutoServis yeni hesap onayı", "text", bodyText);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Resend API returned HTTP " + response.statusCode());
    }

    public void yoneticiOnayKoduGonder(IsletmeHesabi hesap, String kod) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()
                || adminEmail == null || adminEmail.isBlank()) {
            throw new IOException("Email settings are not configured");
        }
        String bodyText = "AutoServis hesap onay kodu: " + kod
                + "\n\nİşletme: " + hesap.getIsletmeAdi()
                + "\nİşletme e-postası: " + hesap.getEposta()
                + "\n\nBu kod 10 dakika geçerlidir. Onay talebini siz başlatmadıysanız kodu kullanmayın.";
        Map<String, Object> body = Map.of("from", from, "to", List.of(adminEmail),
                "subject", "AutoServis hesap onay kodu", "text", bodyText);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Resend API returned HTTP " + response.statusCode());
    }

    public void yoneticiHesapKurtarmaKoduGonder(String kod) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()
                || adminEmail == null || adminEmail.isBlank()) {
            throw new IOException("Email settings are not configured");
        }
        String bodyText = "AutoServis ana yönetici hesabını geri yüklemek için doğrulama kodu: " + kod
                + "\n\nBu kod 10 dakika geçerlidir. Kurtarma işlemini siz başlatmadıysanız kullanmayın.";
        Map<String, Object> body = Map.of("from", from, "to", List.of(adminEmail),
                "subject", "AutoServis yönetici hesabı kurtarma kodu", "text", bodyText);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Resend API returned HTTP " + response.statusCode());
    }
}
