package com.autoservis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HesapOnayTalebiRepository extends JpaRepository<HesapOnayTalebi, Long> {
    Optional<HesapOnayTalebi> findByTokenOzeti(String tokenOzeti);
    Optional<HesapOnayTalebi> findByDurumTokenOzeti(String durumTokenOzeti);
}
