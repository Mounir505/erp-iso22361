/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.controller;

import jakarta.validation.Valid;
import ma.atelier.erp.metier.dto.PrescriptionDto;
import ma.atelier.erp.metier.dto.PrescriptionRequest;
import ma.atelier.erp.metier.service.PrescriptionService;
import ma.atelier.erp.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService service;

    public PrescriptionController(PrescriptionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURE_PRESCRIPTIONS)
    public List<PrescriptionDto> lister(@RequestParam(required = false) Integer admissionId) {
        return service.lister(admissionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ECRITURE_PRESCRIPTIONS)
    public PrescriptionDto creer(@Valid @RequestBody PrescriptionRequest req) {
        return service.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ECRITURE_PRESCRIPTIONS)
    public PrescriptionDto modifier(@PathVariable int id, @Valid @RequestBody PrescriptionRequest req) {
        return service.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ECRITURE_PRESCRIPTIONS)
    public void supprimer(@PathVariable int id) {
        service.supprimer(id);
    }
}
