/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.common;

/**
 * Écriture refusée car le mode dégradé place les données vitales en lecture seule
 * → HTTP 423 (Locked). Cf. livrable 1.5 et ISO 22361 5.3.5.
 */
public class ModeDegradeActifException extends RuntimeException {

    public ModeDegradeActifException(String message) {
        super(message);
    }
}
