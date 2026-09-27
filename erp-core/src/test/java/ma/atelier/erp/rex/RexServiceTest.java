/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.rex;

import ma.atelier.erp.crise.entity.JournalAudit;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.TypesEvenement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Règles du REX indépendantes de la base. */
class RexServiceTest {

    @Test
    @DisplayName("Chronologie : une rafale d'entrées identiques est regroupée ; les décisions jamais")
    void regroupement() {
        Instant t = Instant.parse("2026-10-01T14:00:00Z");
        List<JournalAudit> entrees = List.of(
                entree(t, Niveau.WARNING, TypesEvenement.AUTH_FAILURE),
                entree(t.plusSeconds(1), Niveau.WARNING, TypesEvenement.AUTH_FAILURE),
                entree(t.plusSeconds(2), Niveau.WARNING, TypesEvenement.AUTH_FAILURE),
                entree(t.plusSeconds(3), Niveau.CRITICAL, TypesEvenement.INTEGRITE_FICHIERS),
                entree(t.plusSeconds(4), Niveau.INFO, TypesEvenement.DECISION),
                entree(t.plusSeconds(5), Niveau.INFO, TypesEvenement.DECISION));

        var lignes = RexService.regrouper(entrees);

        assertThat(lignes).hasSize(4);
        assertThat(lignes.getFirst().occurrences()).isEqualTo(3);
        assertThat(lignes.getFirst().fin()).isEqualTo(t.plusSeconds(2));
        assertThat(lignes.get(2).occurrences()).isEqualTo(1);
    }

    @Test
    @DisplayName("Leçons automatiques : détection tardive, absence de décision et de mode dégradé signalées")
    void leconsAutomatiques() {
        IndicateursDto ind = new IndicateursDto(null, null, null, null, null, "automatique",
                600, 12, null, 3_600, 0, 0, false);
        List<String> lecons = new RexService(null, null, null, null, null, null, null)
                .leconsAutomatiques(ind, List.of());

        assertThat(lecons).anyMatch(l -> l.startsWith("Détection tardive"))
                .anyMatch(l -> l.startsWith("Aucune décision"))
                .anyMatch(l -> l.startsWith("Mode dégradé non activé"))
                .anyMatch(l -> l.startsWith("Aucune restauration"));
    }

    private static JournalAudit entree(Instant t, Niveau n, String type) {
        return new JournalAudit(t, n, type, "test", "message", null, null);
    }
}
