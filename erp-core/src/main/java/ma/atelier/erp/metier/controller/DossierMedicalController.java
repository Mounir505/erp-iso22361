/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.controller;

import ma.atelier.erp.metier.dto.DossierMedicalDto;
import ma.atelier.erp.metier.service.DossierMedicalService;
import ma.atelier.erp.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Vue de continuité des soins : dossiers des patients hospitalisés (lisible en mode dégradé). */
@RestController
@RequestMapping("/api/dossiers")
public class DossierMedicalController {

    private final DossierMedicalService service;

    public DossierMedicalController(DossierMedicalService service) {
        this.service = service;
    }

    @GetMapping("/hospitalises")
    @PreAuthorize(Roles.LECTURE_DOSSIERS)
    public List<DossierMedicalDto> hospitalises() {
        return service.dossiersHospitalises();
    }
}
