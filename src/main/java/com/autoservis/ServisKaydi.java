package com.autoservis;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class ServisKaydi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String islem;
    private String aciklama;
    private LocalDate tarih;
    private double iscilikUcreti;
    private double parcaUcreti;
    private String durum;

    @ManyToOne
    private Arac arac;

    public ServisKaydi() {
    }

    public Long getId() {
        return id;
    }

    public String getIslem() {
        return islem;
    }

    public void setIslem(String islem) {
        this.islem = islem;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public LocalDate getTarih() {
        return tarih;
    }

    public void setTarih(LocalDate tarih) {
        this.tarih = tarih;
    }

    public double getIscilikUcreti() {
        return iscilikUcreti;
    }

    public void setIscilikUcreti(double iscilikUcreti) {
        this.iscilikUcreti = iscilikUcreti;
    }

    public double getParcaUcreti() {
        return parcaUcreti;
    }

    public void setParcaUcreti(double parcaUcreti) {
        this.parcaUcreti = parcaUcreti;
    }

    public String getDurum() {
        return durum;
    }

    public void setDurum(String durum) {
        this.durum = durum;
    }

    public Arac getArac() {
        return arac;
    }

    public void setArac(Arac arac) {
        this.arac = arac;
    }

    public double getToplamUcret() {
        return iscilikUcreti + parcaUcreti;
    }
}