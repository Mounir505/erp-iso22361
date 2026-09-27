/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.controller;

import jakarta.validation.Valid;
import ma.atelier.erp.metier.dto.AdmissionDto;
import ma.atelier.erp.metier.dto.AdmissionRequest;
import ma.atelier.erp.metier.service.AdmissionService;
import ma.atelier.erp.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admissions")
public class AdmissionController {

    private final AdmissionService service;

    public AdmissionController(AdmissionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURE_ADMISSIONS)
    public List<AdmissionDto> lister(@RequestParam(required = false) Integer patientId) {
        return service.lister(patientId);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.LECTURE_ADMISSIONS)
    public AdmissionDto lire(@PathVariable int id) {
        return service.lire(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.GESTION_ADMISSIONS)
    public AdmissionDto creer(@Valid @RequestBody AdmissionRequest req) {
        return service.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.GESTION_ADMISSIONS)
    public AdmissionDto modifier(@PathVariable int id, @Valid @RequestBody AdmissionRequest req) {
        return service.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void supprimer(@PathVariable int id) {
        service.supprimer(id);
    }
}
