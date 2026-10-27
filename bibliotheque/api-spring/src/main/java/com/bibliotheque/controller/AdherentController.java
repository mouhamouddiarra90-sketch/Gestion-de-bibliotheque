package com.bibliotheque.controller;

import com.bibliotheque.entity.Adherent;
import com.bibliotheque.repository.AdherentRepository;
import com.bibliotheque.repository.EmpruntRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/adherents")
public class AdherentController {

    private final AdherentRepository adherentRepository;
    private final EmpruntRepository empruntRepository;

    public AdherentController(AdherentRepository adherentRepository,
                              EmpruntRepository empruntRepository) {
        this.adherentRepository = adherentRepository;
        this.empruntRepository = empruntRepository;
    }

    // GET /api/adherents  |  ?recherche=mot (nom ou téléphone)
    @GetMapping
    public List<Adherent> lister(@RequestParam(required = false) String recherche) {
        Sort tri = Sort.by("id");
        if (recherche == null || recherche.isBlank()) {
            return adherentRepository.findAll(tri);
        }
        String motCle = recherche.trim();
        return adherentRepository.findByNomContainingIgnoreCaseOrTelephoneContainingIgnoreCase(
                motCle, motCle, tri);
    }

    @GetMapping("/{id}")
    public Adherent afficher(@PathVariable Integer id) {
        return trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Adherent creer(@Valid @RequestBody Adherent adherent) {
        adherent.setId(null);
        return adherentRepository.save(adherent);
    }

    @PutMapping("/{id}")
    public Adherent modifier(@PathVariable Integer id, @Valid @RequestBody Adherent donnees) {
        Adherent adherent = trouver(id);
        adherent.setNom(donnees.getNom());
        adherent.setTelephone(donnees.getTelephone());
        return adherentRepository.save(adherent);
    }

    // Même règle que pour les livres : on protège l'historique des emprunts
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Integer id) {
        Adherent adherent = trouver(id);
        if (empruntRepository.countByAdherentIdAndDateRetourIsNull(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer un adhérent ayant des emprunts en cours.");
        }
        if (empruntRepository.existsByAdherentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet adhérent a un historique d'emprunts et ne peut pas être supprimé.");
        }
        adherentRepository.delete(adherent);
    }

    private Adherent trouver(Integer id) {
        return adherentRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhérent introuvable."));
    }
}
