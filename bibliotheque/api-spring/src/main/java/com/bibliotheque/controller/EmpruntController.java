package com.bibliotheque.controller;

import com.bibliotheque.entity.Emprunt;
import com.bibliotheque.repository.EmpruntRepository;
import com.bibliotheque.service.EmpruntService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class EmpruntController {

    // Corps JSON attendus
    public record EmpruntRequest(
            @NotNull(message = "livreId est obligatoire") Integer livreId,
            @NotNull(message = "adherentId est obligatoire") Integer adherentId) {
    }

    public record RetourRequest(
            @NotNull(message = "livreId est obligatoire") Integer livreId) {
    }

    private final EmpruntService empruntService;
    private final EmpruntRepository empruntRepository;

    public EmpruntController(EmpruntService empruntService, EmpruntRepository empruntRepository) {
        this.empruntService = empruntService;
        this.empruntRepository = empruntRepository;
    }

    // GET /api/emprunts  |  GET /api/emprunts?enCours=true
    @GetMapping("/emprunts")
    public List<Emprunt> lister(@RequestParam(defaultValue = "false") boolean enCours) {
        return enCours
                ? empruntRepository.findByDateRetourIsNullOrderByDateEmpruntAscIdAsc()
                : empruntRepository.findAll(Sort.by("id"));
    }

    @GetMapping("/emprunts/{id}")
    public Emprunt afficher(@PathVariable Integer id) {
        return empruntRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emprunt introuvable."));
    }

    @PostMapping("/emprunts")
    @ResponseStatus(HttpStatus.CREATED)
    public Emprunt emprunter(@Valid @RequestBody EmpruntRequest requete) {
        return empruntService.emprunter(requete.livreId(), requete.adherentId());
    }

    @PostMapping("/retours")
    public Emprunt retourner(@Valid @RequestBody RetourRequest requete) {
        return empruntService.retourner(requete.livreId());
    }
}
