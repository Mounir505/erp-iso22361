/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import ma.atelier.erp.config.ErpProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Émission et vérification des jetons JWT (HMAC-SHA256).
 * Le secret (Base64, ≥ 256 bits) vient de la variable d'environnement JWT_SECRET.
 */
@Service
public class JwtService {

    private final SecretKey cle;
    private final long expirationMinutes;

    public JwtService(ErpProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET est obligatoire (openssl rand -base64 48)");
        }
        byte[] octets = Decoders.BASE64.decode(secret);
        if (octets.length < 32) {
            throw new IllegalStateException("JWT_SECRET doit faire au moins 256 bits une fois décodé");
        }
        this.cle = Keys.hmacShaKeyFor(octets);
        this.expirationMinutes = props.jwt().expirationMinutes();
    }

    public record JetonEmis(String jeton, Instant expiration) {}

    public JetonEmis emettre(UtilisateurPrincipal u) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(expirationMinutes, ChronoUnit.MINUTES);
        String jeton = Jwts.builder()
                .subject(u.login())
                .claim("uid", u.id())
                .claim("nom", u.nom())
                .claim("role", u.role())
                .claim("crise", u.estRoleDeCrise())
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(expiration))
                .signWith(cle)
                .compact();
        return new JetonEmis(jeton, expiration);
    }

    /** Vérifie signature et expiration ; renvoie vide si le jeton est invalide. */
    public Optional<UtilisateurPrincipal> verifier(String jeton) {
        try {
            Claims c = Jwts.parser().verifyWith(cle).build().parseSignedClaims(jeton).getPayload();
            return Optional.of(new UtilisateurPrincipal(
                    c.get("uid", Integer.class), c.getSubject(), c.get("nom", String.class),
                    c.get("role", String.class), Boolean.TRUE.equals(c.get("crise", Boolean.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
