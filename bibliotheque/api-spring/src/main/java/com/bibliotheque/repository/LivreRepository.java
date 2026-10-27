package com.bibliotheque.repository;

import com.bibliotheque.entity.Livre;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LivreRepository extends JpaRepository<Livre, Integer> {

    List<Livre> findByTitreContainingIgnoreCaseOrAuteurContainingIgnoreCase(
            String titre, String auteur);

    // Équivalent du "SELECT ... FOR UPDATE" de la version JDBC
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Livre l where l.id = :id")
    Optional<Livre> chargerPourMiseAJour(@Param("id") Integer id);
}
