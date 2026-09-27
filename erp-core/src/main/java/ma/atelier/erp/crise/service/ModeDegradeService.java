/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.common.ModeDegradeActifException;
import ma.atelier.erp.crise.entity.ModeDegrade;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.repository.ModeDegradeRepository;
import ma.atelier.erp.metier.entity.Patient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mode dégradé — continuité des soins (livrable 1.5, ISO 22361 5.3.5).
 * Actif, il place en LECTURE SEULE les données vitales des patients hospitalisés :
 * la consultation reste possible, toute modification est refusée (HTTP 423) et tracée.
 */
@Service
public class ModeDegradeService {

    private static final String SOURCE = "mode-degrade";

    private final ModeDegradeRepository repository;
    private final JournalAuditService journal;

    public ModeDegradeService(ModeDegradeRepository repository, JournalAuditService journal) {
        this.repository = repository;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public ModeDegrade etat() {
        return repository.findById(ModeDegrade.ID_UNIQUE).orElseGet(ModeDegrade::new);
    }

    /**
     * Active ou désactive le mode dégradé. Opération idempotente : un changement d'état
     * est tracé (WARNING à l'activation, INFO au retour à la normale).
     *
     * @return {@code true} si l'état a effectivement changé
     */
    @Transactional
    public boolean definir(boolean actif, String motif) {
        ModeDegrade mode = repository.findById(ModeDegrade.ID_UNIQUE).orElseGet(ModeDegrade::new);
        if (mode.isActif() == actif) {
            return false;
        }
        if (actif) {
            mode.activer();
        } else {
            mode.desactiver();
        }
        repository.save(mode);
        journal.enregistrer(actif ? Niveau.WARNING : Niveau.INFO,
                actif ? TypesEvenement.MODE_DEGRADE_ON : TypesEvenement.MODE_DEGRADE_OFF, SOURCE,
                actif ? "Mode dégradé activé : dossiers des patients hospitalisés en lecture seule"
                      : "Mode dégradé désactivé : retour au fonctionnement nominal",
                Details.de("motif", motif, "lectureSeule", mode.isLectureSeule()));
        return true;
    }

    /**
     * Garde à appeler avant toute écriture touchant les données vitales d'un patient.
     *
     * @throws ModeDegradeActifException si le mode dégradé interdit l'opération
     */
    public void verifierEcriturePermise(Patient patient, String operation) {
        if (etat().bloqueEcriture(patient)) {
            journal.enregistrer(Niveau.WARNING, TypesEvenement.ECRITURE_BLOQUEE, SOURCE,
                    "Écriture refusée : mode dégradé actif (lecture seule)",
                    Details.de("patientId", patient.getId(), "operation", operation));
            throw new ModeDegradeActifException(
                    "Mode dégradé actif : les données des patients hospitalisés sont en lecture seule");
        }
    }
}
