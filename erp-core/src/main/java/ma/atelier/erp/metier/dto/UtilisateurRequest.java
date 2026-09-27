/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création / modification d'un utilisateur. En modification, un mot de passe vide conserve l'actuel.
 */
public record UtilisateurRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+") String login,
        @Size(max = 100) String motDePasse,
        @NotBlank String role) {}
