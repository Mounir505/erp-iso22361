/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Format unique des erreurs renvoyées par l'API.
 *
 * @param champs erreurs de validation par champ (absent si non pertinent)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String erreur, String message, String chemin,
                       Map<String, String> champs) {

    public static ApiError of(int status, String erreur, String message, String chemin) {
        return new ApiError(Instant.now(), status, erreur, message, chemin, null);
    }
}
