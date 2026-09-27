/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.common;

import jakarta.servlet.http.HttpServletRequest;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.TypesEvenement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduction homogène des exceptions en réponses {@link ApiError}.
 * Les refus d'accès sont journalisés (signal potentiel d'une tentative d'intrusion).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final JournalAuditService journal;

    public GlobalExceptionHandler(JournalAuditService journal) {
        this.journal = journal;
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    ResponseEntity<ApiError> introuvable(RessourceIntrouvableException e, HttpServletRequest req) {
        return reponse(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(RegleMetierException.class)
    ResponseEntity<ApiError> regleMetier(RegleMetierException e, HttpServletRequest req) {
        return reponse(HttpStatus.CONFLICT, e.getMessage(), req);
    }

    @ExceptionHandler(ModeDegradeActifException.class)
    ResponseEntity<ApiError> modeDegrade(ModeDegradeActifException e, HttpServletRequest req) {
        return reponse(HttpStatus.LOCKED, e.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentification(AuthenticationException e, HttpServletRequest req) {
        return reponse(HttpStatus.UNAUTHORIZED, e.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accesRefuse(AccessDeniedException e, HttpServletRequest req) {
        journal.enregistrer(Niveau.WARNING, TypesEvenement.ACCESS_DENIED, "erp-core",
                "Accès refusé (droits insuffisants)", Details.de("methode", req.getMethod(), "chemin", req.getRequestURI()));
        return reponse(HttpStatus.FORBIDDEN, "Droits insuffisants pour cette opération", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> champs = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> champs.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(java.time.Instant.now(), 400, "Requête invalide",
                "Certains champs sont invalides", req.getRequestURI(), champs));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> illisible(Exception e, HttpServletRequest req) {
        return reponse(HttpStatus.BAD_REQUEST, "Corps ou paramètre de requête illisible", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrite(DataIntegrityViolationException e, HttpServletRequest req) {
        return reponse(HttpStatus.CONFLICT, "Opération contraire à l'intégrité des données", req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> routeInconnue(NoResourceFoundException e, HttpServletRequest req) {
        return reponse(HttpStatus.NOT_FOUND, "Ressource inexistante", req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> inattendue(Exception e, HttpServletRequest req) {
        log.error("Erreur inattendue sur {} {}", req.getMethod(), req.getRequestURI(), e);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur", req);
    }

    private static ResponseEntity<ApiError> reponse(HttpStatus statut, String message, HttpServletRequest req) {
        return ResponseEntity.status(statut)
                .body(ApiError.of(statut.value(), statut.getReasonPhrase(), message, req.getRequestURI()));
    }
}
