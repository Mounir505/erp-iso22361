/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import ma.atelier.erp.metier.entity.Utilisateur;

/** Vue d'un utilisateur — le hash du mot de passe n'est jamais exposé. */
public record UtilisateurDto(Integer id, String nom, String login, String role, boolean estRoleDeCrise) {

    public static UtilisateurDto de(Utilisateur u) {
        return new UtilisateurDto(u.getId(), u.getNom(), u.getLogin(), u.getRole().getLibelle(),
                u.getRole().isEstRoleDeCrise());
    }
}
