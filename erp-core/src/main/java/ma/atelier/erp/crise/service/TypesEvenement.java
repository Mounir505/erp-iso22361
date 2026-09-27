/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.service;

/** Valeurs du champ {@code event_type} du journal (contrat d'interface 2.1). */
public final class TypesEvenement {

    // Sécurité / accès
    public static final String AUTH_SUCCESS = "auth_success";
    public static final String AUTH_FAILURE = "auth_failure";
    public static final String AUTH_LOGOUT = "auth_logout";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String DOSSIER_ACCESS = "medical_record_access";
    public static final String DATA_CHANGE = "data_change";

    // Seuils (livrable 1.3)
    public static final String SEUIL_AUTH = "auth_failure_threshold";
    public static final String SERVICE_INDISPONIBLE = "service_unavailable";
    public static final String SERVICE_RETABLI = "service_restored";
    public static final String INTEGRITE_FICHIERS = "file_integrity";
    public static final String ACCES_MASSE = "mass_record_access";
    public static final String CONFIG_SEUILS = "threshold_config_changed";
    public static final String INTEGRITE_REFERENCE = "integrity_rebaseline";

    // Cellule de crise et continuité
    public static final String CELLULE_ACTIVEE = "crisis_cell_activated";
    public static final String CELLULE_CLOTUREE = "crisis_cell_closed";
    public static final String EVENEMENT_RATTACHE = "crisis_event_attached";
    public static final String DECISION = "crisis_decision";
    public static final String MODE_DEGRADE_ON = "degraded_mode_on";
    public static final String MODE_DEGRADE_OFF = "degraded_mode_off";
    public static final String ECRITURE_BLOQUEE = "degraded_mode_write_blocked";

    // Rétablissement et apprentissage
    public static final String SAUVEGARDE = "database_backup";
    public static final String RESTAURATION = "database_restore";
    public static final String REX = "rex_generated";
    public static final String EXERCICE = "exercise_injection";

    private TypesEvenement() {}
}
