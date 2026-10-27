package com.bibliotheque.repository;

import com.bibliotheque.entity.Emprunt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpruntRepository extends JpaRepository<Emprunt, Integer> {

    List<Emprunt> findByDateRetourIsNullOrderByDateEmpruntAscIdAsc();

    long countByAdherentIdAndDateRetourIsNull(Integer adherentId);

    Optional<Emprunt> findByLivreIdAndDateRetourIsNull(Integer livreId);

    boolean existsByLivreId(Integer livreId);

    boolean existsByAdherentId(Integer adherentId);
}
