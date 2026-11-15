package com.autoservis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.UUID;

@Entity
public class Arac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String plaka;
    private String marka;
    private String model;
    private int yil;
    private String telefon;
    @Column(unique = true, length = 17)
    private String sasiNumarasi;
    private String durum;

    private LocalDateTime servisBaslangicZamani;
    private LocalDateTime servisBitisZamani;

    @Column(unique = true, length = 36)
    private String takipAnahtari;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "isletme_id")
    private IsletmeHesabi isletme;

    public Arac() {
    }

    @PrePersist
    public void takipAnahtariOlustur() {
        if (takipAnahtari == null || takipAnahtari.isBlank()) {
            takipAnahtari = UUID.randomUUID().toString();
        }
        if (servisBaslangicZamani == null) {
            servisBaslangicZamani = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        }
        if (servisBittiMi(durum) && servisBitisZamani == null) {
            servisBitisZamani = servisBaslangicZamani;
        }
    }

    public Long getId() {
        return id;
    }

    public String getPlaka() {
        return plaka;
    }

    public void setPlaka(String plaka) {
        this.plaka = plaka;
    }

    public String getMarka() {
        return marka;
    }

    public void setMarka(String marka) {
        this.marka = marka;
    }

    public int getYil() {
        return yil;
    }

    public void setYil(int yil) {
        this.yil = yil;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getSasiNumarasi() {
        return sasiNumarasi;
    }

    public void setSasiNumarasi(String sasiNumarasi) {
        this.sasiNumarasi = sasiNumarasi == null ? null
                : sasiNumarasi.replaceAll("[\\s-]", "").toUpperCase();
    }

    public String getDurum() {
        return durum;
    }

    public void setDurum(String durum) {
        this.durum = durum;
    }

    public LocalDateTime getServisBaslangicZamani() {
        return servisBaslangicZamani;
    }

    public LocalDateTime getServisBitisZamani() {
        return servisBitisZamani;
    }

    public void servisDurumunuGuncelle(String yeniDurum) {
        LocalDateTime simdi = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        boolean oncekiDurumBitti = servisBittiMi(this.durum);
        boolean yeniDurumBitti = servisBittiMi(yeniDurum);

        if (servisBaslangicZamani == null || (oncekiDurumBitti && !yeniDurumBitti)) {
            servisBaslangicZamani = simdi;
        }
        if (yeniDurumBitti && (!oncekiDurumBitti || servisBitisZamani == null)) {
            servisBitisZamani = simdi;
        } else if (!yeniDurumBitti) {
            servisBitisZamani = null;
        }
        this.durum = yeniDurum;
    }

    private boolean servisBittiMi(String durum) {
        return "Araç hazır".equalsIgnoreCase(durum) || "Teslim edildi".equalsIgnoreCase(durum);
    }

    public String getTakipAnahtari() {
        return takipAnahtari;
    }

    public void setTakipAnahtari(String takipAnahtari) {
        this.takipAnahtari = takipAnahtari;
    }

    public IsletmeHesabi getIsletme() { return isletme; }

    public void setIsletme(IsletmeHesabi isletme) { this.isletme = isletme; }
}
