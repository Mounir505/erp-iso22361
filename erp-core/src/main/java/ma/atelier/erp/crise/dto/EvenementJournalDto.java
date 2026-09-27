/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ma.atelier.erp.crise.entity.JournalAudit;
import ma.atelier.erp.crise.entity.Niveau;

import java.time.Instant;
import java.util.Map;

/**
 * Ligne de journal au format EXACT du contrat d'interface (livrable 2.1) :
 * { timestamp, level, event_type, source, user, message, details }.
 * L'{@code id} est ajouté pour permettre au dashboard un polling incrémental.
 */
@JsonPropertyOrder({"id", "timestamp", "level", "event_type", "source", "user", "message", "details"})
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EvenementJournalDto(
        Integer id,
        Instant timestamp,
        Niveau level,
        @JsonProperty("event_type") String eventType,
        String source,
        String user,
        String message,
        Map<String, Object> details) {

    public static EvenementJournalDto de(JournalAudit j) {
        return new EvenementJournalDto(j.getId(), j.getTimestamp(), j.getNiveau(), j.getTypeEvenement(),
                j.getSource(), j.getUtilisateur() == null ? null : j.getUtilisateur().getLogin(),
                j.getMessage(), j.getDetails());
    }
}
