/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.common;

import java.util.LinkedHashMap;
import java.util.Map;

/** Construction concise du champ libre {@code details} du journal (valeurs nulles admises). */
public final class Details {

    private Details() {}

    public static Map<String, Object> de(Object... clesValeurs) {
        if (clesValeurs.length % 2 != 0) {
            throw new IllegalArgumentException("Nombre pair d'arguments attendu (clé, valeur, …)");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < clesValeurs.length; i += 2) {
            m.put(String.valueOf(clesValeurs[i]), clesValeurs[i + 1]);
        }
        return m;
    }
}
