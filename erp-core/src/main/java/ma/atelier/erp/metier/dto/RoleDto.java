/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.dto;

import ma.atelier.erp.metier.entity.Role;

public record RoleDto(Integer id, String libelle, boolean estRoleDeCrise) {

    public static RoleDto de(Role r) {
        return new RoleDto(r.getId(), r.getLibelle(), r.isEstRoleDeCrise());
    }
}
