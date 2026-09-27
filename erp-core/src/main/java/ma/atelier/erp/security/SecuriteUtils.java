/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Accès à l'utilisateur courant depuis les services. */
public final class SecuriteUtils {

    private SecuriteUtils() {}

    public static Optional<UtilisateurPrincipal> utilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UtilisateurPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    /** Nom affichable de l'acteur courant, ou « systeme » pour les traitements planifiés. */
    public static String nomCourant() {
        return utilisateurCourant().map(UtilisateurPrincipal::nom).orElse("systeme");
    }
}
