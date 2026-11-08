package com.autoservis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AracRepository extends JpaRepository<Arac, Long> {
    java.util.Optional<Arac> findBySasiNumarasi(String sasiNumarasi);
    java.util.List<Arac> findAllByIsletme_IdOrderByIdDesc(Long isletmeId);
    java.util.Optional<Arac> findByIdAndIsletme_Id(Long id, Long isletmeId);
    long countByIsletme_Id(Long isletmeId);
    java.util.List<Arac> findAllByIsletmeIsNull();
}
