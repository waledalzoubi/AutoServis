package com.autoservis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServisKaydiRepository extends JpaRepository<ServisKaydi, Long> {

    List<ServisKaydi> findByAracId(Long aracId);
}