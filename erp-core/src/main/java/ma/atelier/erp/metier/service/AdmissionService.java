/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.common.RessourceIntrouvableException;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.ModeDegradeService;
import ma.atelier.erp.crise.service.TypesEvenement;
import ma.atelier.erp.metier.dto.AdmissionDto;
import ma.atelier.erp.metier.dto.AdmissionRequest;
import ma.atelier.erp.metier.entity.Admission;
import ma.atelier.erp.metier.entity.Patient;
import ma.atelier.erp.metier.entity.StatutAdmission;
import ma.atelier.erp.metier.entity.StatutPatient;
import ma.atelier.erp.metier.repository.AdmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admissions. Règle de cohérence : une admission EN_COURS rend le patient HOSPITALISE ; la
 * clôture de sa dernière admission en cours le fait passer SORTI. En mode dégradé, le statut
 * d'un patient hospitalisé est figé (sinon il sortirait du périmètre protégé).
 */
@Service
@Transactional
public class AdmissionService {

    private static final String SOURCE = "erp-core";

    private final AdmissionRepository repository;
    private final PatientService patientService;
    private final ModeDegradeService modeDegrade;
    private final JournalAuditService journal;

    public AdmissionService(AdmissionRepository repository, PatientService patientService,
                            ModeDegradeService modeDegrade, JournalAuditService journal) {
        this.repository = repository;
        this.patientService = patientService;
        this.modeDegrade = modeDegrade;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public List<AdmissionDto> lister(Integer patientId) {
        List<Admission> admissions = patientId == null
                ? repository.findAllByOrderByDateEntreeDesc()
                : repository.findByPatientIdOrderByDateEntreeDesc(patientId);
        return admissions.stream().map(AdmissionDto::de).toList();
    }

    @Transactional(readOnly = true)
    public AdmissionDto lire(int id) {
        return AdmissionDto.de(trouver(id));
    }

    public AdmissionDto creer(AdmissionRequest req) {
        Patient p = patientService.trouver(req.patientId());
        Admission a = repository.save(new Admission(p,
                req.dateEntree() == null ? Instant.now() : req.dateEntree(), req.service(), req.statut()));
        if (a.getStatut() == StatutAdmission.EN_COURS) {
            p.setStatut(StatutPatient.HOSPITALISE);
        }
        tracer(a.getId(), p.getId(), "creation");
        return AdmissionDto.de(a);
    }

    public AdmissionDto modifier(int id, AdmissionRequest req) {
        Admission a = trouver(id);
        Patient p = a.getPatient();
        boolean fermeture = a.getStatut() == StatutAdmission.EN_COURS && req.statut() != StatutAdmission.EN_COURS;
        if (fermeture) {
            modeDegrade.verifierEcriturePermise(p, "clôture admission");
        }
        a.setService(req.service());
        a.setStatut(req.statut());
        if (req.dateEntree() != null) {
            a.setDateEntree(req.dateEntree());
        }
        recalculerStatutPatient(a);
        tracer(id, p.getId(), "modification");
        return AdmissionDto.de(a);
    }

    public void supprimer(int id) {
        Admission a = trouver(id);
        if (a.getStatut() == StatutAdmission.EN_COURS) {
            modeDegrade.verifierEcriturePermise(a.getPatient(), "suppression admission");
        }
        repository.delete(a);
        a.setStatut(StatutAdmission.ANNULEE);
        recalculerStatutPatient(a);
        tracer(id, a.getPatient().getId(), "suppression");
    }

    Admission trouver(int id) {
        return repository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Admission", id));
    }

    private void recalculerStatutPatient(Admission modifiee) {
        Patient p = modifiee.getPatient();
        boolean autreEnCours = repository.existsByPatientIdAndStatutAndIdNot(p.getId(), StatutAdmission.EN_COURS,
                modifiee.getId());
        if (modifiee.getStatut() == StatutAdmission.EN_COURS || autreEnCours) {
            p.setStatut(StatutPatient.HOSPITALISE);
        } else if (p.estHospitalise()) {
            p.setStatut(StatutPatient.SORTI);
        }
    }

    private void tracer(int id, int patientId, String operation) {
        journal.enregistrer(Niveau.INFO, TypesEvenement.DATA_CHANGE, SOURCE, "Admission : " + operation,
                Details.de("entite", "Admission", "id", id, "patientId", patientId, "operation", operation));
    }
}
