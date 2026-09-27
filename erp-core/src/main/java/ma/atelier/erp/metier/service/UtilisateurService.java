/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.service;

import ma.atelier.erp.common.Details;
import ma.atelier.erp.common.RegleMetierException;
import ma.atelier.erp.common.RessourceIntrouvableException;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.TypesEvenement;
import ma.atelier.erp.metier.dto.RoleDto;
import ma.atelier.erp.metier.dto.UtilisateurDto;
import ma.atelier.erp.metier.dto.UtilisateurRequest;
import ma.atelier.erp.metier.entity.Role;
import ma.atelier.erp.metier.entity.Utilisateur;
import ma.atelier.erp.metier.repository.RoleRepository;
import ma.atelier.erp.metier.repository.UtilisateurRepository;
import ma.atelier.erp.security.SecuriteUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Gestion des comptes et de l'attribution des rôles (réservée à l'administrateur). */
@Service
@Transactional
public class UtilisateurService {

    private static final String SOURCE = "erp-core";

    private final UtilisateurRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JournalAuditService journal;

    public UtilisateurService(UtilisateurRepository repository, RoleRepository roleRepository,
                              PasswordEncoder encoder, JournalAuditService journal) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDto> lister() {
        return repository.findAll().stream().map(UtilisateurDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleDto> roles() {
        return roleRepository.findAll().stream().map(RoleDto::de).toList();
    }

    public UtilisateurDto creer(UtilisateurRequest req) {
        if (repository.existsByLogin(req.login())) {
            throw new RegleMetierException("Ce login est déjà utilisé");
        }
        exigerMotDePasse(req.motDePasse());
        Utilisateur u = repository.save(new Utilisateur(req.nom(), req.login(),
                encoder.encode(req.motDePasse()), role(req.role())));
        journal.enregistrer(Niveau.INFO, TypesEvenement.DATA_CHANGE, SOURCE, "Utilisateur créé",
                Details.de("entite", "Utilisateur", "id", u.getId(), "operation", "creation", "role", req.role()));
        return UtilisateurDto.de(u);
    }

    public UtilisateurDto modifier(int id, UtilisateurRequest req) {
        Utilisateur u = trouver(id);
        if (!u.getLogin().equals(req.login()) && repository.existsByLogin(req.login())) {
            throw new RegleMetierException("Ce login est déjà utilisé");
        }
        String ancienRole = u.getRole().getLibelle();
        u.setNom(req.nom());
        u.setLogin(req.login());
        u.setRole(role(req.role()));
        if (req.motDePasse() != null && !req.motDePasse().isBlank()) {
            exigerMotDePasse(req.motDePasse());
            u.setMotDePasse(encoder.encode(req.motDePasse()));
        }
        boolean changementRole = !Objects.equals(ancienRole, req.role());
        // Un changement de rôle est une modification de droits : niveau WARNING
        journal.enregistrer(changementRole ? Niveau.WARNING : Niveau.INFO, TypesEvenement.DATA_CHANGE, SOURCE,
                changementRole ? "Rôle d'un utilisateur modifié" : "Utilisateur modifié",
                Details.de("entite", "Utilisateur", "id", id, "operation", "modification",
                        "roleAvant", ancienRole, "roleApres", req.role()));
        return UtilisateurDto.de(u);
    }

    public void supprimer(int id) {
        Utilisateur u = trouver(id);
        if (SecuriteUtils.utilisateurCourant().map(p -> p.id().equals(id)).orElse(false)) {
            throw new RegleMetierException("Un administrateur ne peut pas supprimer son propre compte");
        }
        repository.delete(u);
        journal.enregistrer(Niveau.WARNING, TypesEvenement.DATA_CHANGE, SOURCE, "Utilisateur supprimé",
                Details.de("entite", "Utilisateur", "id", id, "operation", "suppression", "login", u.getLogin()));
    }

    private Utilisateur trouver(int id) {
        return repository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", id));
    }

    private Role role(String libelle) {
        return roleRepository.findByLibelle(libelle)
                .orElseThrow(() -> new RegleMetierException("Rôle inconnu : " + libelle));
    }

    private static void exigerMotDePasse(String mdp) {
        if (mdp == null || mdp.length() < 8) {
            throw new RegleMetierException("Le mot de passe doit contenir au moins 8 caractères");
        }
    }
}
