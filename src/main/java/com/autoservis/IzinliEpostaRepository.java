package com.autoservis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IzinliEpostaRepository extends JpaRepository<IzinliEposta, Long> {
    boolean existsByEposta(String eposta);
    Optional<IzinliEposta> findByEposta(String eposta);
    List<IzinliEposta> findAllByOrderByEpostaAsc();
}
