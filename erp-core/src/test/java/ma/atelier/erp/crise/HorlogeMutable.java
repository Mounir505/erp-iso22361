/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise;

import java.time.*;

/** Horloge de test que l'on fait avancer à la main (fenêtres glissantes). */
public class HorlogeMutable extends Clock {

    private Instant maintenant;

    public HorlogeMutable(Instant depart) {
        this.maintenant = depart;
    }

    public void avancer(Duration d) {
        maintenant = maintenant.plus(d);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return maintenant; }
}
