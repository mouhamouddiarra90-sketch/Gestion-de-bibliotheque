package com.bibliotheque.controller;

import com.bibliotheque.entity.Livre;
import com.bibliotheque.repository.EmpruntRepository;
import com.bibliotheque.repository.LivreRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/livres")
public class LivreController {

    private final LivreRepository livreRepository;
    private final EmpruntRepository empruntRepository;

    public LivreController(LivreRepository livreRepository, EmpruntRepository empruntRepository) {
        this.livreRepository = livreRepository;
        this.empruntRepository = empruntRepository;
    }

    // GET /api/livres  |  ?disponible=true  |  ?recherche=mot  |  les deux ensemble
    @GetMapping
    public List<Livre> lister(@RequestParam(required = false) Boolean disponible,
                              @RequestParam(required = false) String recherche) {
        List<Livre> livres = (recherche == null || recherche.isBlank())
                ? livreRepository.findAll(Sort.by("id"))
                : livreRepository.findByTitreContainingIgnoreCaseOrAuteurContainingIgnoreCase(
                        recherche, recherche);
        if (disponible != null) {
            return livres.stream().filter(l -> l.isDisponible() == disponible).toList();
        }
        return livres;
    }

    @GetMapping("/{id}")
    public Livre afficher(@PathVariable Integer id) {
        return trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Livre creer(@Valid @RequestBody Livre livre) {
        livre.setId(null);          // un nouveau livre ne reprend jamais un id envoyé par le client
        livre.setDisponible(true);  // la disponibilité est gérée par les emprunts
        return livreRepository.save(livre);
    }

    @PutMapping("/{id}")
    public Livre modifier(@PathVariable Integer id, @Valid @RequestBody Livre donnees) {
        Livre livre = trouver(id);
        livre.setTitre(donnees.getTitre());
        livre.setAuteur(donnees.getAuteur());
        return livreRepository.save(livre);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Integer id) {
        Livre livre = trouver(id);
        if (!livre.isDisponible()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer un livre actuellement emprunté.");
        }
        if (empruntRepository.existsByLivreId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce livre a un historique d'emprunts et ne peut pas être supprimé.");
        }
        livreRepository.delete(livre);
    }

    private Livre trouver(Integer id) {
        return livreRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livre introuvable."));
    }
}
