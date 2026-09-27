/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.common.RessourceIntrouvableException;
import ma.atelier.erp.crise.entity.ModeDegrade;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.DetecteurDeSeuilService;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.ModeDegradeService;
import ma.atelier.erp.crise.service.TypesEvenement;
import ma.atelier.erp.metier.dto.DossierMedicalDto;
import ma.atelier.erp.metier.dto.DossierMedicalRequest;
import ma.atelier.erp.metier.entity.DossierMedical;
import ma.atelier.erp.metier.entity.StatutPatient;
import ma.atelier.erp.metier.repository.DossierMedicalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dossiers médicaux — données VITALES (livrable 1.1).
 * Chaque consultation est journalisée et comptée par le détecteur (accès anormal en masse).
 * Chaque modification passe par la garde du mode dégradé.
 */
@Service
@Transactional
public class DossierMedicalService {

    private static final String SOURCE = "erp-core";

    private final DossierMedicalRepository repository;
    private final ModeDegradeService modeDegrade;
    private final DetecteurDeSeuilService detecteur;
    private final JournalAuditService journal;

    public DossierMedicalService(DossierMedicalRepository repository, ModeDegradeService modeDegrade,
                                 DetecteurDeSeuilService detecteur, JournalAuditService journal) {
        this.repository = repository;
        this.modeDegrade = modeDegrade;
        this.detecteur = detecteur;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public DossierMedicalDto lireParPatient(int patientId) {
        DossierMedical d = repository.findByPatientId(patientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Dossier médical du patient", patientId));
        journal.enregistrer(Niveau.INFO, TypesEvenement.DOSSIER_ACCESS, SOURCE, "Consultation d'un dossier médical",
                Details.de("patientId", patientId, "dossierId", d.getId()));
        detecteur.signalerAccesDossier();
        return DossierMedicalDto.de(d, modeDegrade.etat().bloqueEcriture(d.getPatient()));
    }

    /** Vue de continuité : dossiers des patients hospitalisés (consultables même en mode dégradé). */
    @Transactional(readOnly = true)
    public List<DossierMedicalDto> dossiersHospitalises() {
        ModeDegrade mode = modeDegrade.etat();
        List<DossierMedicalDto> dossiers = repository.findByPatientStatutOrderByPatientNom(StatutPatient.HOSPITALISE)
                .stream().map(d -> DossierMedicalDto.de(d, mode.bloqueEcriture(d.getPatient()))).toList();
        journal.enregistrer(Niveau.INFO, TypesEvenement.DOSSIER_ACCESS, SOURCE,
                "Consultation de la liste des dossiers des patients hospitalisés",
                Details.de("nombre", dossiers.size(), "modeDegrade", mode.isActif()));
        return dossiers;
    }

    public DossierMedicalDto modifier(int patientId, DossierMedicalRequest req) {
        DossierMedical d = repository.findByPatientId(patientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Dossier médical du patient", patientId));
        modeDegrade.verifierEcriturePermise(d.getPatient(), "modification dossier médical");
        d.setAntecedents(req.antecedents());
        d.setObservations(req.observations());
        journal.enregistrer(Niveau.INFO, TypesEvenement.DATA_CHANGE, SOURCE, "Dossier médical modifié",
                Details.de("entite", "DossierMedical", "id", d.getId(), "patientId", patientId,
                        "operation", "modification"));
        return DossierMedicalDto.de(d, false);
    }
}
