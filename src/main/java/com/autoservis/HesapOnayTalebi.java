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

    @Column(nullable = false, unique = true, length = 64)
    private String tokenOzeti;

    @Column(nullable = false)
    private Instant gecerlilikZamani;

    public Long getId() { return id; }
    public IsletmeHesabi getHesap() { return hesap; }
    public void setHesap(IsletmeHesabi hesap) { this.hesap = hesap; }
    public String getTokenOzeti() { return tokenOzeti; }
    public void setTokenOzeti(String tokenOzeti) { this.tokenOzeti = tokenOzeti; }
    public Instant getGecerlilikZamani() { return gecerlilikZamani; }
    public void setGecerlilikZamani(Instant gecerlilikZamani) { this.gecerlilikZamani = gecerlilikZamani; }
}
