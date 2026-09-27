/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.common.RessourceIntrouvableException;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.ModeDegradeService;
import ma.atelier.erp.crise.service.TypesEvenement;
import ma.atelier.erp.metier.dto.PatientDto;
import ma.atelier.erp.metier.dto.PatientRequest;
import ma.atelier.erp.metier.entity.DossierMedical;
import ma.atelier.erp.metier.entity.Patient;
import ma.atelier.erp.metier.entity.StatutPatient;
import ma.atelier.erp.metier.repository.DossierMedicalRepository;
import ma.atelier.erp.metier.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Patients. La création ouvre un dossier médical vide (Patient 1 → 1 DossierMedical).
 * En mode dégradé, les données d'un patient hospitalisé ne peuvent être ni modifiées ni supprimées.
 */
@Service
@Transactional
public class PatientService {

    private static final String SOURCE = "erp-core";

    private final PatientRepository repository;
    private final DossierMedicalRepository dossierRepository;
    private final ModeDegradeService modeDegrade;
    private final JournalAuditService journal;

    public PatientService(PatientRepository repository, DossierMedicalRepository dossierRepository,
                          ModeDegradeService modeDegrade, JournalAuditService journal) {
        this.repository = repository;
        this.dossierRepository = dossierRepository;
        this.modeDegrade = modeDegrade;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public List<PatientDto> rechercher(String q, StatutPatient statut) {
        String filtre = (q == null || q.isBlank()) ? null : q.trim();
        return repository.rechercher(filtre, statut).stream().map(PatientDto::de).toList();
    }

    @Transactional(readOnly = true)
    public PatientDto lire(int id) {
        return PatientDto.de(trouver(id));
    }

    public PatientDto creer(PatientRequest req) {
        Patient p = repository.save(new Patient(req.nom(), req.dateNaissance(), req.statut()));
        dossierRepository.save(new DossierMedical(p, null, null));
        tracer(p.getId(), "creation");
        return PatientDto.de(p);
    }

    public PatientDto modifier(int id, PatientRequest req) {
        Patient p = trouver(id);
        modeDegrade.verifierEcriturePermise(p, "modification patient");
        p.setNom(req.nom());
        p.setDateNaissance(req.dateNaissance());
        p.setStatut(req.statut());
        tracer(id, "modification");
        return PatientDto.de(p);
    }

    public void supprimer(int id) {
        Patient p = trouver(id);
        modeDegrade.verifierEcriturePermise(p, "suppression patient");
        repository.delete(p);
        tracer(id, "suppression");
    }

    Patient trouver(int id) {
        return repository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Patient", id));
    }

    private void tracer(int id, String operation) {
        journal.enregistrer(operation.equals("suppression") ? Niveau.WARNING : Niveau.INFO,
                TypesEvenement.DATA_CHANGE, SOURCE, "Patient : " + operation,
                Details.de("entite", "Patient", "id", id, "operation", operation));
    }
}
