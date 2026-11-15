package com.autoservis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sifre_sifirlama_kodlari")
public class SifreSifirlamaKodu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "isletme_hesabi_id", nullable = false, unique = true)
    private IsletmeHesabi hesap;

    @Column(nullable = false, length = 120)
    private String kodOzeti;

    @Column(nullable = false)
    private Instant olusturulmaZamani;

    @Column(nullable = false)
    private Instant gecerlilikZamani;

    @Column(nullable = false)
    private int hataliDenemeSayisi;

    public Long getId() { return id; }
    public IsletmeHesabi getHesap() { return hesap; }
    public void setHesap(IsletmeHesabi hesap) { this.hesap = hesap; }
    public String getKodOzeti() { return kodOzeti; }
    public void setKodOzeti(String kodOzeti) { this.kodOzeti = kodOzeti; }
    public Instant getOlusturulmaZamani() { return olusturulmaZamani; }
    public void setOlusturulmaZamani(Instant olusturulmaZamani) { this.olusturulmaZamani = olusturulmaZamani; }
    public Instant getGecerlilikZamani() { return gecerlilikZamani; }
    public void setGecerlilikZamani(Instant gecerlilikZamani) { this.gecerlilikZamani = gecerlilikZamani; }
    public int getHataliDenemeSayisi() { return hataliDenemeSayisi; }
    public void setHataliDenemeSayisi(int hataliDenemeSayisi) { this.hataliDenemeSayisi = hataliDenemeSayisi; }
}
