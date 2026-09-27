/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.controller;

import jakarta.validation.Valid;
import ma.atelier.erp.metier.dto.*;
import ma.atelier.erp.metier.entity.StatutPatient;
import ma.atelier.erp.metier.service.DossierMedicalService;
import ma.atelier.erp.metier.service.PatientService;
import ma.atelier.erp.security.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Patients et dossiers médicaux (sous-ressource : Patient 1 → 1 DossierMedical). */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final DossierMedicalService dossierService;

    public PatientController(PatientService patientService, DossierMedicalService dossierService) {
        this.patientService = patientService;
        this.dossierService = dossierService;
    }

    @GetMapping
    @PreAuthorize(Roles.PERSONNEL_SOIGNANT_OU_ADMIN)
    public List<PatientDto> rechercher(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) StatutPatient statut) {
        return patientService.rechercher(q, statut);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.PERSONNEL_SOIGNANT_OU_ADMIN)
    public PatientDto lire(@PathVariable int id) {
        return patientService.lire(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.GESTION_PATIENTS)
    public PatientDto creer(@Valid @RequestBody PatientRequest req) {
        return patientService.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.GESTION_PATIENTS)
    public PatientDto modifier(@PathVariable int id, @Valid @RequestBody PatientRequest req) {
        return patientService.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void supprimer(@PathVariable int id) {
        patientService.supprimer(id);
    }

    // ------------------------------------------------------ dossier médical --

    @GetMapping("/{id}/dossier")
    @PreAuthorize(Roles.LECTURE_DOSSIERS)
    public DossierMedicalDto dossier(@PathVariable int id) {
        return dossierService.lireParPatient(id);
    }

    @PutMapping("/{id}/dossier")
    @PreAuthorize(Roles.ECRITURE_DOSSIERS)
    public DossierMedicalDto modifierDossier(@PathVariable int id, @Valid @RequestBody DossierMedicalRequest req) {
        return dossierService.modifier(id, req);
    }
}
