/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.controller;

import jakarta.validation.Valid;
import ma.atelier.erp.metier.dto.RoleDto;
import ma.atelier.erp.metier.dto.UtilisateurDto;
import ma.atelier.erp.metier.dto.UtilisateurRequest;
import ma.atelier.erp.metier.service.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administration des comptes et des rôles (ADMIN uniquement). */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private final UtilisateurService service;

    public UtilisateurController(UtilisateurService service) {
        this.service = service;
    }

    @GetMapping("/utilisateurs")
    public List<UtilisateurDto> lister() {
        return service.lister();
    }

    @GetMapping("/roles")
    public List<RoleDto> roles() {
        return service.roles();
    }

    @PostMapping("/utilisateurs")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurDto creer(@Valid @RequestBody UtilisateurRequest req) {
        return service.creer(req);
    }

    @PutMapping("/utilisateurs/{id}")
    public UtilisateurDto modifier(@PathVariable int id, @Valid @RequestBody UtilisateurRequest req) {
        return service.modifier(id, req);
    }

    @DeleteMapping("/utilisateurs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable int id) {
        service.supprimer(id);
    }
}
