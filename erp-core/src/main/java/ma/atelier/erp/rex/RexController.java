/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.rex;

import ma.atelier.erp.security.Roles;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Export du REX.
 * <pre>
 *   GET  /api/rex/{celluleId}                     aperçu JSON
 *   POST /api/rex/{celluleId}?format=json|pdf     rapport final avec les leçons de l'équipe
 * </pre>
 */
@RestController
@RequestMapping("/api/rex")
@PreAuthorize(Roles.CELLULE_OU_ADMIN)
public class RexController {

    private final RexService service;
    private final RexPdfExporter pdf;

    public RexController(RexService service, RexPdfExporter pdf) {
        this.service = service;
        this.pdf = pdf;
    }

    @GetMapping("/{celluleId}")
    public RapportRexDto apercu(@PathVariable int celluleId) {
        return service.rapport(celluleId, List.of());
    }

    @PostMapping("/{celluleId}")
    public ResponseEntity<?> exporter(@PathVariable int celluleId,
                                      @RequestParam(defaultValue = "json") String format,
                                      @RequestBody(required = false) RapportRexDto.LeconsRequest req) {
        List<String> lecons = req == null ? List.of() : req.lecons();
        RapportRexDto rapport = service.rapport(celluleId, lecons);
        boolean enPdf = "pdf".equalsIgnoreCase(format);
        service.tracerGeneration(celluleId, enPdf ? "pdf" : "json", rapport.leconsEquipe());

        String nom = "rex-crise-" + celluleId + (enPdf ? ".pdf" : ".json");
        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentDisposition(ContentDisposition.attachment().filename(nom).build());
        if (enPdf) {
            return ResponseEntity.ok().headers(entetes).contentType(MediaType.APPLICATION_PDF)
                    .body(pdf.exporter(rapport));
        }
        return ResponseEntity.ok().headers(entetes).contentType(MediaType.APPLICATION_JSON).body(rapport);
    }
}
