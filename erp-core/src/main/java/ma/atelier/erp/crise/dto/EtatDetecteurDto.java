/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise.dto;

/**
 * Seuils configurés (livrable 1.3) et valeurs courantes des compteurs, pour le dashboard.
 */
public record EtatDetecteurDto(
        int seuilEchecsAuth,
        int fenetre,
        int echecsAuthDansFenetre,
        int seuilIndisponibiliteSecondes,
        int seuilAlterationsIntegrite,
        int seuilAccesMasseDossiers,
        int fenetreAccesMasseSecondes,
        int maxAccesDossiersParUtilisateur) {}
