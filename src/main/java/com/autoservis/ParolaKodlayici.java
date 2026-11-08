package com.autoservis;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ParolaKodlayici {
    private static final int ITERASYON = 210_000;
    private static final int TUZ_UZUNLUGU = 16;
    private static final int OZET_BIT_UZUNLUGU = 256;
    private final SecureRandom random = new SecureRandom();

    public String kodla(String parola) {
        byte[] tuz = new byte[TUZ_UZUNLUGU];
        random.nextBytes(tuz);
        byte[] ozet = derive(parola, tuz, ITERASYON);
        return "pbkdf2$" + ITERASYON + "$"
                + Base64.getEncoder().encodeToString(tuz) + "$"
                + Base64.getEncoder().encodeToString(ozet);
    }

    public boolean dogrula(String parola, String kayitliOzet) {
        if (parola == null || kayitliOzet == null) return false;
        try {
            String[] bolumler = kayitliOzet.split("\\$");
            if (bolumler.length != 4 || !"pbkdf2".equals(bolumler[0])) return false;
            int iterasyon = Integer.parseInt(bolumler[1]);
            byte[] tuz = Base64.getDecoder().decode(bolumler[2]);
            byte[] beklenen = Base64.getDecoder().decode(bolumler[3]);
            byte[] bulunan = derive(parola, tuz, iterasyon);
            return MessageDigest.isEqual(beklenen, bulunan);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] derive(String parola, byte[] tuz, int iterasyon) {
        char[] karakterler = parola.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(karakterler, tuz, iterasyon, OZET_BIT_UZUNLUGU);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Parola özeti oluşturulamadı.", ex);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(karakterler, '\0');
        }
    }
}
