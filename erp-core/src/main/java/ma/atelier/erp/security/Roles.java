/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.security;

/**
 * Libellés des rôles (table {@code role}) et expressions d'autorisation réutilisables.
 * Rôles de crise conformes au livrable 1.4 (pilote, technique, communication, secrétaire).
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String MEDECIN = "MEDECIN";
    public static final String INFIRMIER = "INFIRMIER";
    public static final String AGENT_ADMISSION = "AGENT_ADMISSION";
    public static final String PHARMACIEN = "PHARMACIEN";
    public static final String PILOTE_CRISE = "PILOTE_CRISE";
    public static final String RESP_TECHNIQUE = "RESP_TECHNIQUE";
    public static final String RESP_COMMUNICATION = "RESP_COMMUNICATION";
    public static final String SECRETAIRE_CRISE = "SECRETAIRE_CRISE";

    // --- Expressions @PreAuthorize (principe du moindre privilège) ---------------------
    public static final String PERSONNEL_SOIGNANT_OU_ADMIN =
            "hasAnyRole('ADMIN','MEDECIN','INFIRMIER','AGENT_ADMISSION','PHARMACIEN')";
    public static final String GESTION_PATIENTS = "hasAnyRole('ADMIN','MEDECIN','AGENT_ADMISSION')";
    public static final String LECTURE_DOSSIERS = "hasAnyRole('MEDECIN','INFIRMIER')";
    public static final String ECRITURE_DOSSIERS = "hasRole('MEDECIN')";
    public static final String LECTURE_ADMISSIONS = "hasAnyRole('ADMIN','MEDECIN','INFIRMIER','AGENT_ADMISSION')";
    public static final String GESTION_ADMISSIONS = "hasAnyRole('ADMIN','MEDECIN','AGENT_ADMISSION')";
    public static final String LECTURE_PRESCRIPTIONS = "hasAnyRole('MEDECIN','INFIRMIER','PHARMACIEN')";
    public static final String ECRITURE_PRESCRIPTIONS = "hasRole('MEDECIN')";

    /** Conscience partagée : toute la cellule de crise + l'administrateur SI. */
    public static final String CELLULE_OU_ADMIN =
            "hasAnyRole('ADMIN','PILOTE_CRISE','RESP_TECHNIQUE','RESP_COMMUNICATION','SECRETAIRE_CRISE')";
    public static final String PILOTE = "hasRole('PILOTE_CRISE')";
    public static final String BASCULE_MODE_DEGRADE = "hasAnyRole('PILOTE_CRISE','RESP_TECHNIQUE')";
    public static final String SAISIE_DECISIONS = "hasAnyRole('PILOTE_CRISE','SECRETAIRE_CRISE')";
    public static final String CONFIG_SEUILS = "hasAnyRole('ADMIN','PILOTE_CRISE','RESP_TECHNIQUE')";
    public static final String TECHNIQUE = "hasAnyRole('ADMIN','RESP_TECHNIQUE')";

    private Roles() {}
}
