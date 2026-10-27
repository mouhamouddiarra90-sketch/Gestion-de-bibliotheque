package com.bibliotheque.service;

import com.bibliotheque.entity.Adherent;
import com.bibliotheque.entity.Emprunt;
import com.bibliotheque.entity.Livre;
import com.bibliotheque.repository.AdherentRepository;
import com.bibliotheque.repository.EmpruntRepository;
import com.bibliotheque.repository.LivreRepository;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmpruntService {
    public static final int MAX_EMPRUNTS = 3;

    private final LivreRepository livreRepository;
    private final AdherentRepository adherentRepository;
    private final EmpruntRepository empruntRepository;

    public EmpruntService(LivreRepository livreRepository,
                          AdherentRepository adherentRepository,
                          EmpruntRepository empruntRepository) {
        this.livreRepository = livreRepository;
        this.adherentRepository = adherentRepository;
        this.empruntRepository = empruntRepository;
    }

    // @Transactional : l'emprunt et la mise à jour du livre réussissent ensemble ou pas du tout
    @Transactional
    public Emprunt emprunter(Integer livreId, Integer adherentId) {
        Livre livre = livreRepository.chargerPourMiseAJour(livreId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Livre introuvable."));
        Adherent adherent = adherentRepository.findById(adherentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Adhérent introuvable."));

        if (!livre.isDisponible()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce livre est déjà emprunté.");
        }
        if (empruntRepository.countByAdherentIdAndDateRetourIsNull(adherentId) >= MAX_EMPRUNTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    adherent.getNom() + " a atteint la limite de " + MAX_EMPRUNTS + " livres.");
        }

        livre.setDisponible(false);

        Emprunt emprunt = new Emprunt();
        emprunt.setLivre(livre);
        emprunt.setAdherent(adherent);
        emprunt.setDateEmprunt(LocalDate.now());
        return empruntRepository.save(emprunt);
    }

    @Transactional
    public Emprunt retourner(Integer livreId) {
        Livre livre = livreRepository.chargerPourMiseAJour(livreId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Livre introuvable."));
        Emprunt emprunt = empruntRepository.findByLivreIdAndDateRetourIsNull(livreId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Ce livre n'est pas emprunté."));

        emprunt.setDateRetour(LocalDate.now());
        livre.setDisponible(true);
        return emprunt;
    }
}
