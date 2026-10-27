package com.bibliotheque.repository;

import com.bibliotheque.entity.Adherent;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdherentRepository extends JpaRepository<Adherent, Integer> {

    List<Adherent> findByNomContainingIgnoreCaseOrTelephoneContainingIgnoreCase(
            String nom, String telephone, Sort tri);
}
