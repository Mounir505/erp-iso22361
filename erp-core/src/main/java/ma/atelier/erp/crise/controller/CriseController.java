/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.controller;

import jakarta.validation.Valid;
import ma.atelier.erp.crise.dto.CriseDtos.*;
import ma.atelier.erp.crise.dto.EtatDetecteurDto;
import ma.atelier.erp.crise.repository.EvenementRepository;
import ma.atelier.erp.crise.service.CelluleDeCriseService;
import ma.atelier.erp.crise.service.ControleIntegrite;
import ma.atelier.erp.crise.service.DetecteurDeSeuilService;
import ma.atelier.erp.rex.RexService;
import ma.atelier.erp.rex.SituationDto;
import ma.atelier.erp.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * API de la cellule de crise : situation, cellule, décisions, événements, seuils, intégrité.
 * Lecture : toute la cellule + ADMIN (conscience partagée, ISO 22361 5.3.4.4).
 */
@RestController
@RequestMapping("/api/crise")
@PreAuthorize(Roles.CELLULE_OU_ADMIN)
public class CriseController {

    private final CelluleDeCriseService celluleService;
    private final DetecteurDeSeuilService detecteur;
    private final ControleIntegrite controleIntegrite;
    private final EvenementRepository evenementRepository;
    private final RexService rexService;

    public CriseController(CelluleDeCriseService celluleService, DetecteurDeSeuilService detecteur,
                           ControleIntegrite controleIntegrite, EvenementRepository evenementRepository,
                           RexService rexService) {
        this.celluleService = celluleService;
        this.detecteur = detecteur;
        this.controleIntegrite = controleIntegrite;
        this.evenementRepository = evenementRepository;
        this.rexService = rexService;
    }

    /** Vue synthétique pour le dashboard : niveau global, cellule, indicateurs, compteurs. */
    @GetMapping("/situation")
    public SituationDto situation() {
        return rexService.situation();
    }

    // ------------------------------------------------------------ cellule --

    @GetMapping("/cellules")
    public List<CelluleDto> cellules() {
        return celluleService.lister().stream().map(CelluleDto::de).toList();
    }

    @GetMapping("/cellules/active")
    public ResponseEntity<CelluleDto> active() {
        return celluleService.active().map(c -> ResponseEntity.ok(CelluleDto.de(c)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/cellules/{id}")
    public CelluleDto cellule(@PathVariable int id) {
        return CelluleDto.de(celluleService.trouver(id));
    }

    @PostMapping("/cellules/activer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.PILOTE)
    public CelluleDto activer(@Valid @RequestBody(required = false) MotifRequest req) {
        return CelluleDto.de(celluleService.activerManuellement(req == null ? null : req.motif()));
    }

    @PostMapping("/cellules/{id}/cloturer")
    @PreAuthorize(Roles.PILOTE)
    public CelluleDto cloturer(@PathVariable int id, @Valid @RequestBody(required = false) MotifRequest req) {
        return CelluleDto.de(celluleService.cloturer(id, req == null ? null : req.motif()));
    }

    @GetMapping("/cellules/{id}/decisions")
    @Transactional(readOnly = true)
    public List<DecisionDto> decisions(@PathVariable int id) {
        return celluleService.decisions(id).stream().map(DecisionDto::de).toList();
    }

    @PostMapping("/cellules/{id}/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.SAISIE_DECISIONS)
    public DecisionDto ajouterDecision(@PathVariable int id, @Valid @RequestBody DecisionRequest req) {
        return DecisionDto.de(celluleService.ajouterDecision(id, req.libelle(), req.auteur()));
    }

    // --------------------------------------------------------- événements --

    @GetMapping("/evenements")
    @Transactional(readOnly = true)
    public List<EvenementDto> evenements() {
        return evenementRepository.findTop100ByOrderByTimestampDesc().stream().map(EvenementDto::de).toList();
    }

    // ------------------------------------------------------ seuils / intégrité --

    @GetMapping("/detecteur")
    public EtatDetecteurDto detecteur() {
        return detecteur.etat();
    }

    @PutMapping("/detecteur")
    @PreAuthorize(Roles.CONFIG_SEUILS)
    public EtatDetecteurDto configurer(@Valid @RequestBody SeuilsRequest req) {
        detecteur.configurer(req.seuilEchecsAuth(), req.fenetre());
        return detecteur.etat();
    }

    @GetMapping("/integrite")
    public ControleIntegrite.Resultat integrite() {
        return controleIntegrite.dernierResultat();
    }

    /** Après restauration des fichiers sains : nouvelle référence d'intégrité (rétablissement). */
    @PostMapping("/integrite/reference")
    @PreAuthorize(Roles.TECHNIQUE)
    public ControleIntegrite.Resultat nouvelleReference() throws IOException {
        return controleIntegrite.reinitialiserReference();
    }
}
