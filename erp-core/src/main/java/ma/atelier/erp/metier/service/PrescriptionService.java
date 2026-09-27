/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.common.RessourceIntrouvableException;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.TypesEvenement;
import ma.atelier.erp.metier.dto.PrescriptionDto;
import ma.atelier.erp.metier.dto.PrescriptionRequest;
import ma.atelier.erp.metier.entity.Admission;
import ma.atelier.erp.metier.entity.Prescription;
import ma.atelier.erp.metier.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Prescriptions (pharmacie, donnée vitale : risque d'erreur de médication, livrable 1.1). */
@Service
@Transactional
public class PrescriptionService {

    private static final String SOURCE = "erp-core";

    private final PrescriptionRepository repository;
    private final AdmissionService admissionService;
    private final JournalAuditService journal;

    public PrescriptionService(PrescriptionRepository repository, AdmissionService admissionService,
                               JournalAuditService journal) {
        this.repository = repository;
        this.admissionService = admissionService;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> lister(Integer admissionId) {
        List<Prescription> liste = admissionId == null
                ? repository.findAllByOrderByDateDesc()
                : repository.findByAdmissionIdOrderByDateDesc(admissionId);
        return liste.stream().map(PrescriptionDto::de).toList();
    }

    public PrescriptionDto creer(PrescriptionRequest req) {
        Admission a = admissionService.trouver(req.admissionId());
        Prescription p = repository.save(new Prescription(a, req.medicament(), req.posologie(),
                req.date() == null ? Instant.now() : req.date()));
        tracer(p.getId(), "creation");
        return PrescriptionDto.de(p);
    }

    public PrescriptionDto modifier(int id, PrescriptionRequest req) {
        Prescription p = trouver(id);
        p.setMedicament(req.medicament());
        p.setPosologie(req.posologie());
        if (req.date() != null) {
            p.setDate(req.date());
        }
        tracer(id, "modification");
        return PrescriptionDto.de(p);
    }

    public void supprimer(int id) {
        repository.delete(trouver(id));
        tracer(id, "suppression");
    }

    private Prescription trouver(int id) {
        return repository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Prescription", id));
    }

    private void tracer(int id, String operation) {
        journal.enregistrer(Niveau.INFO, TypesEvenement.DATA_CHANGE, SOURCE, "Prescription : " + operation,
                Details.de("entite", "Prescription", "id", id, "operation", operation));
    }
}
