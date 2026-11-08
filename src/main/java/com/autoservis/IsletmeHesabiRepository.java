package com.autoservis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IsletmeHesabiRepository extends JpaRepository<IsletmeHesabi, Long> {
    Optional<IsletmeHesabi> findByEposta(String eposta);
    boolean existsByEposta(String eposta);
}
