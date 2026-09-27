/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.rex;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ma.atelier.erp.crise.dto.CriseDtos.DecisionDto;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Mise en page PDF du rapport REX (OpenPDF). Horodatages affichés en UTC. */
@Component
public class RexPdfExporter {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
    private static final Color BLEU = new Color(0x1e, 0x3a, 0x5f);
    private static final String COPYRIGHT = "© 2026 Équipe projet n°3 — ENSA. Tous droits réservés";
    private static final Color GRIS = new Color(0xee, 0xf2, 0xf7);

    private final Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLEU);
    private final Font section = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BLEU);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private final Font petit = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private final Font gras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    public byte[] exporter(RapportRexDto r) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 42, 42);
        PdfWriter.getInstance(doc, out);
        HeaderFooter pied = new HeaderFooter(new Phrase(COPYRIGHT + " - page ", petit), true);
        pied.setAlignment(Element.ALIGN_CENTER);
        pied.setBorder(Rectangle.NO_BORDER);
        doc.setFooter(pied);
        doc.open();

        doc.add(new Paragraph("Retour d'expérience (REX) - Crise n°" + r.cellule().id(), titre));
        doc.add(new Paragraph("Mini-ERP hospitalier - atelier ISO 22361 (lab isolé, données fictives). "
                + "Généré le " + date(r.genereLe()) + " par " + r.generePar() + ".", petit));
        doc.add(new Paragraph("Références : ISO 22361 5.3.7 (amélioration continue) et 9.6 (évaluation, "
                + "apprentissage).", petit));

        IndicateursDto i = r.indicateurs();
        titreSection(doc, "1. Indicateurs de réponse");
        PdfPTable t = tableau(new float[]{3, 4});
        ligne(t, "Statut", i.enCours() ? "Crise en cours (rapport intermédiaire)" : "Crise clôturée");
        ligne(t, "Déclenchement", i.typeDeclenchement());
        ligne(t, "Premier signal suspect", date(i.premierSignal()));
        ligne(t, "Seuil de crise franchi", date(i.declenchement()));
        ligne(t, "Activation de la cellule", date(i.activation()));
        ligne(t, "Première décision", date(i.premiereDecision()));
        ligne(t, "Clôture", date(i.cloture()));
        ligne(t, "Temps de détection", duree(i.tempsDetectionSecondes()));
        ligne(t, "Temps d'activation", i.tempsActivationMs() + " ms");
        ligne(t, "Délai de première décision",
                i.tempsPremiereDecisionSecondes() == null ? "-" : duree(i.tempsPremiereDecisionSecondes()));
        ligne(t, "Durée de la crise", duree(i.dureeCriseSecondes()));
        ligne(t, "Durée en mode dégradé", duree(i.dureeModeDegradeSecondes()));
        ligne(t, "Nombre de décisions", String.valueOf(i.nombreDecisions()));
        doc.add(t);

        titreSection(doc, "2. Décisions de la cellule de crise");
        if (r.decisions().isEmpty()) {
            doc.add(new Paragraph("Aucune décision enregistrée.", normal));
        } else {
            PdfPTable d = tableau(new float[]{2.2f, 2, 6});
            entete(d, "Horodatage", "Auteur", "Décision");
            for (DecisionDto dec : r.decisions()) {
                cellules(d, date(dec.timestamp()), dec.auteur(), dec.libelle());
            }
            doc.add(d);
        }

        titreSection(doc, "3. Chronologie (journal d'audit)");
        PdfPTable c = tableau(new float[]{2.2f, 1.3f, 2.4f, 5.5f});
        entete(c, "Horodatage", "Niveau", "Type", "Message");
        for (RapportRexDto.LigneChronologie l : r.chronologie()) {
            String msg = l.message() + (l.occurrences() > 1
                    ? " (x" + l.occurrences() + " jusqu'à " + date(l.fin()) + ")" : "")
                    + (l.user() == null ? "" : " - " + l.user());
            cellules(c, date(l.debut()), l.niveau().name(), l.type(), msg);
        }
        doc.add(c);

        titreSection(doc, "4. Leçons tirées");
        doc.add(new Paragraph("Constats automatiques :", gras));
        liste(doc, r.leconsAutomatiques());
        doc.add(new Paragraph("Leçons de l'équipe :", gras));
        if (r.leconsEquipe().isEmpty()) {
            doc.add(new Paragraph("(aucune saisie)", normal));
        } else {
            liste(doc, r.leconsEquipe());
        }

        doc.close();
        return out.toByteArray();
    }

    // ------------------------------------------------------------- outils --

    private void titreSection(Document doc, String texte) {
        Paragraph p = new Paragraph(texte, section);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private static PdfPTable tableau(float[] largeurs) {
        PdfPTable t = new PdfPTable(largeurs);
        t.setWidthPercentage(100);
        return t;
    }

    private void entete(PdfPTable t, String... titres) {
        for (String s : titres) {
            PdfPCell cell = new PdfPCell(new Phrase(s, gras));
            cell.setBackgroundColor(GRIS);
            cell.setPadding(4);
            t.addCell(cell);
        }
    }

    private void cellules(PdfPTable t, String... valeurs) {
        for (String v : valeurs) {
            PdfPCell cell = new PdfPCell(new Phrase(v == null ? "-" : v, petit));
            cell.setPadding(3);
            t.addCell(cell);
        }
    }

    private void ligne(PdfPTable t, String libelle, String valeur) {
        PdfPCell a = new PdfPCell(new Phrase(libelle, gras));
        a.setBackgroundColor(GRIS);
        a.setPadding(4);
        t.addCell(a);
        PdfPCell b = new PdfPCell(new Phrase(valeur == null ? "-" : valeur, normal));
        b.setPadding(4);
        t.addCell(b);
    }

    private void liste(Document doc, java.util.List<String> items) {
        List l = new List(List.UNORDERED);
        items.forEach(s -> l.add(new ListItem(s, normal)));
        doc.add(l);
    }

    private static String date(Instant i) {
        return i == null ? "-" : FORMAT.format(i);
    }

    private static String duree(long s) {
        if (s < 60) {
            return s + " s";
        }
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? "%d h %02d min %02d s".formatted(h, m, sec) : "%d min %02d s".formatted(m, sec);
    }
}
