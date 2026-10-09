package com.autoservis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

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
    private String durum;

    @Column(unique = true, length = 36)
    private String takipAnahtari;

    public Arac() {
    }

    @PrePersist
    public void takipAnahtariOlustur() {
        if (takipAnahtari == null || takipAnahtari.isBlank()) {
            takipAnahtari = UUID.randomUUID().toString();
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

    public String getDurum() {
        return durum;
    }

    public void setDurum(String durum) {
        this.durum = durum;
    }

    public String getTakipAnahtari() {
        return takipAnahtari;
    }

    public void setTakipAnahtari(String takipAnahtari) {
        this.takipAnahtari = takipAnahtari;
    }
}