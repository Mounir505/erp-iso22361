/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** DTO d'authentification. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Size(max = 50) String login,
                               @NotBlank @Size(max = 100) String motDePasse) {}

    public record LoginResponse(String token, Instant expiration, UtilisateurDto utilisateur) {}
}
