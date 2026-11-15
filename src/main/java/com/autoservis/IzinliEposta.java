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
@Table(name = "izinli_epostalar")
public class IzinliEposta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 190)
    private String eposta;

    public Long getId() { return id; }
    public String getEposta() { return eposta; }
    public void setEposta(String eposta) {
        this.eposta = eposta == null ? null : eposta.trim().toLowerCase(Locale.ROOT);
    }

    @PrePersist
    @PreUpdate
    void normalizeEposta() {
        if (eposta != null) eposta = eposta.trim().toLowerCase(Locale.ROOT);
    }
}
