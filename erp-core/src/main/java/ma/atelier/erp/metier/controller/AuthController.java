/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.metier.controller;

import jakarta.validation.Valid;
import ma.atelier.erp.metier.dto.AuthDtos.LoginRequest;
import ma.atelier.erp.metier.dto.AuthDtos.LoginResponse;
import ma.atelier.erp.metier.dto.UtilisateurDto;
import ma.atelier.erp.metier.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.connecter(req.login(), req.motDePasse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        service.deconnecter();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UtilisateurDto moi() {
        return service.moi();
    }
}
