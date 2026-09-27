/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.DetecteurDeSeuilService;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.TypesEvenement;
import ma.atelier.erp.metier.dto.AuthDtos.LoginResponse;
import ma.atelier.erp.metier.dto.UtilisateurDto;
import ma.atelier.erp.metier.entity.Utilisateur;
import ma.atelier.erp.metier.repository.UtilisateurRepository;
import ma.atelier.erp.security.JwtService;
import ma.atelier.erp.security.SecuriteUtils;
import ma.atelier.erp.security.UtilisateurPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentification : chaque succès / échec est journalisé, et chaque échec alimente le
 * compteur du détecteur de seuils (> 20 échecs / min → alerte, livrable 1.3).
 */
@Service
public class AuthService {

    private static final String SOURCE = "auth";

    private final UtilisateurRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final JournalAuditService journal;
    private final DetecteurDeSeuilService detecteur;
    /** Hash factice : le temps de réponse ne révèle pas si le login existe. */
    private final String hashFactice;

    public AuthService(UtilisateurRepository repository, PasswordEncoder encoder, JwtService jwtService,
                       JournalAuditService journal, DetecteurDeSeuilService detecteur) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.journal = journal;
        this.detecteur = detecteur;
        this.hashFactice = encoder.encode("hash-factice-anti-enumeration");
    }

    public LoginResponse connecter(String login, String motDePasse) {
        Utilisateur u = repository.findByLogin(login).orElse(null);
        boolean valide;
        if (u == null) {
            encoder.matches(motDePasse, hashFactice);   // même coût qu'une vraie vérification
            valide = false;
        } else {
            valide = u.authentifier(motDePasse, encoder);
        }
        if (!valide) {
            journal.enregistrer(Niveau.WARNING, TypesEvenement.AUTH_FAILURE, SOURCE,
                    "Échec d'authentification", Details.de("loginTente", login));
            detecteur.signalerEchecAuthentification();
            throw new BadCredentialsException("Identifiants invalides");
        }
        UtilisateurPrincipal p = new UtilisateurPrincipal(u.getId(), u.getLogin(), u.getNom(),
                u.getRole().getLibelle(), u.getRole().isEstRoleDeCrise());
        JwtService.JetonEmis jeton = jwtService.emettre(p);
        journal.enregistrerPour(p, Niveau.INFO, TypesEvenement.AUTH_SUCCESS, SOURCE, "Connexion réussie",
                Details.de("role", p.role()));
        return new LoginResponse(jeton.jeton(), jeton.expiration(), UtilisateurDto.de(u));
    }

    public void deconnecter() {
        SecuriteUtils.utilisateurCourant().ifPresent(p -> journal.enregistrer(Niveau.INFO,
                TypesEvenement.AUTH_LOGOUT, SOURCE, "Déconnexion", null));
    }

    public UtilisateurDto moi() {
        UtilisateurPrincipal p = SecuriteUtils.utilisateurCourant()
                .orElseThrow(() -> new BadCredentialsException("Non authentifié"));
        return new UtilisateurDto(p.id(), p.nom(), p.login(), p.role(), p.estRoleDeCrise());
    }
}
