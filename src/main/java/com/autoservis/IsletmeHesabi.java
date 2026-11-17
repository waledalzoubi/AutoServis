package com.autoservis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.util.Locale;

@Entity
@Table(name = "isletme_hesaplari")
public class IsletmeHesabi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String isletmeAdi;

    @Column(nullable = false, unique = true, length = 190)
    private String eposta;

    @Column(length = 30)
    private String telefon;

    @Column(nullable = false, length = 200)
    private String parolaOzeti;

    @Column
    private Boolean onaylandi = true;

    @Column
    private Boolean aktif = true;

    public Long getId() { return id; }
    public String getIsletmeAdi() { return isletmeAdi; }
    public void setIsletmeAdi(String isletmeAdi) { this.isletmeAdi = isletmeAdi == null ? null : isletmeAdi.trim(); }
    public String getEposta() { return eposta; }
    public void setEposta(String eposta) { this.eposta = eposta == null ? null : eposta.trim().toLowerCase(Locale.ROOT); }
    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon == null || telefon.isBlank() ? null : telefon.trim(); }
    public String getParolaOzeti() { return parolaOzeti; }
    public void setParolaOzeti(String parolaOzeti) { this.parolaOzeti = parolaOzeti; }
    public boolean isOnaylandi() { return onaylandi == null || onaylandi; }
    public void setOnaylandi(boolean onaylandi) { this.onaylandi = onaylandi; }
    public boolean isAktif() { return aktif == null || aktif; }
    public void setAktif(boolean aktif) { this.aktif = aktif; }

    @PrePersist
    @PreUpdate
    void normalizeEposta() {
        if (eposta != null) eposta = eposta.trim().toLowerCase(Locale.ROOT);
    }
}
