package com.autoservis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AracRepository extends JpaRepository<Arac, Long> {
    java.util.Optional<Arac> findBySasiNumarasi(String sasiNumarasi);
}
