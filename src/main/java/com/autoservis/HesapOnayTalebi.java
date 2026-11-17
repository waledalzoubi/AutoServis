package com.autoservis;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "hesap_onay_talepleri")
public class HesapOnayTalebi {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hesap_id", nullable = false, unique = true)
    private IsletmeHesabi hesap;

    @Column(unique = true, length = 64)
    private String tokenOzeti;

    // Nullable for rows created before approval-status polling was introduced.
    // New signup requests always populate this value in AdminAuthController.
    @Column(unique = true, length = 64)
    private String durumTokenOzeti;

    @Column(nullable = false)
    private Instant gecerlilikZamani;

    @Column(length = 200)
    private String yoneticiKodOzeti;

    private Instant yoneticiKodSonKullanma;
    private Instant yoneticiKodGonderilmeZamani;

    private Integer yoneticiKodYanlisDenemeSayisi = 0;

    public Long getId() { return id; }
    public IsletmeHesabi getHesap() { return hesap; }
    public void setHesap(IsletmeHesabi hesap) { this.hesap = hesap; }
    public String getTokenOzeti() { return tokenOzeti; }
    public void setTokenOzeti(String tokenOzeti) { this.tokenOzeti = tokenOzeti; }
    public String getDurumTokenOzeti() { return durumTokenOzeti; }
    public void setDurumTokenOzeti(String durumTokenOzeti) { this.durumTokenOzeti = durumTokenOzeti; }
    public Instant getGecerlilikZamani() { return gecerlilikZamani; }
    public void setGecerlilikZamani(Instant gecerlilikZamani) { this.gecerlilikZamani = gecerlilikZamani; }
    public String getYoneticiKodOzeti() { return yoneticiKodOzeti; }
    public void setYoneticiKodOzeti(String yoneticiKodOzeti) { this.yoneticiKodOzeti = yoneticiKodOzeti; }
    public Instant getYoneticiKodSonKullanma() { return yoneticiKodSonKullanma; }
    public void setYoneticiKodSonKullanma(Instant yoneticiKodSonKullanma) { this.yoneticiKodSonKullanma = yoneticiKodSonKullanma; }
    public Instant getYoneticiKodGonderilmeZamani() { return yoneticiKodGonderilmeZamani; }
    public void setYoneticiKodGonderilmeZamani(Instant yoneticiKodGonderilmeZamani) { this.yoneticiKodGonderilmeZamani = yoneticiKodGonderilmeZamani; }
    public int getYoneticiKodYanlisDenemeSayisi() { return yoneticiKodYanlisDenemeSayisi == null ? 0 : yoneticiKodYanlisDenemeSayisi; }
    public void setYoneticiKodYanlisDenemeSayisi(int yoneticiKodYanlisDenemeSayisi) { this.yoneticiKodYanlisDenemeSayisi = yoneticiKodYanlisDenemeSayisi; }
}
