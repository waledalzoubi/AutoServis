package com.autoservis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SifreSifirlamaKoduRepository extends JpaRepository<SifreSifirlamaKodu, Long> {
    Optional<SifreSifirlamaKodu> findByHesap_Id(Long hesapId);
    void deleteByHesap_Id(Long hesapId);
}
