/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
package ma.atelier.erp.crise;

import ma.atelier.erp.common.ModeDegradeActifException;
import ma.atelier.erp.crise.entity.ModeDegrade;
import ma.atelier.erp.crise.entity.Niveau;
import ma.atelier.erp.crise.repository.ModeDegradeRepository;
import ma.atelier.erp.crise.service.JournalAuditService;
import ma.atelier.erp.crise.service.ModeDegradeService;
import ma.atelier.erp.crise.service.TypesEvenement;
import ma.atelier.erp.metier.entity.Patient;
import ma.atelier.erp.metier.entity.StatutPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Bascule en mode dégradé et garde de lecture seule (livrable 1.5). */
class ModeDegradeServiceTest {

    private final ModeDegrade mode = new ModeDegrade();
    private JournalAuditService journal;
    private ModeDegradeService service;

    private final Patient hospitalise = new Patient("Fictif A", LocalDate.of(1960, 1, 1), StatutPatient.HOSPITALISE);
    private final Patient ambulatoire = new Patient("Fictif B", LocalDate.of(1990, 1, 1), StatutPatient.AMBULATOIRE);

    @BeforeEach
    void preparer() {
        ModeDegradeRepository repo = mock(ModeDegradeRepository.class);
        when(repo.findById(ModeDegrade.ID_UNIQUE)).thenReturn(Optional.of(mode));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        journal = mock(JournalAuditService.class);
        service = new ModeDegradeService(repo, journal);
    }

    @Test
    @DisplayName("Mode inactif : toute écriture est permise")
    void inactifPermetTout() {
        assertThatNoException().isThrownBy(() -> service.verifierEcriturePermise(hospitalise, "test"));
    }

    @Test
    @DisplayName("Mode actif : dossier d'un patient hospitalisé en lecture seule (refus tracé)")
    void actifBloqueHospitalises() {
        service.definir(true, "exercice");

        assertThatThrownBy(() -> service.verifierEcriturePermise(hospitalise, "modification dossier"))
                .isInstanceOf(ModeDegradeActifException.class);
        verify(journal).enregistrer(eq(Niveau.WARNING), eq(TypesEvenement.ECRITURE_BLOQUEE), anyString(),
                anyString(), anyMap());
    }

    @Test
    @DisplayName("Mode actif : les patients non hospitalisés restent modifiables")
    void actifLaisseLesAutres() {
        service.definir(true, "exercice");
        assertThatNoException().isThrownBy(() -> service.verifierEcriturePermise(ambulatoire, "test"));
    }

    @Test
    @DisplayName("Activation tracée en WARNING, idempotente, puis retour à la normale tracé en INFO")
    void basculeIdempotenteEtTracee() {
        assertThat(service.definir(true, "crise")).isTrue();
        assertThat(mode.isActif()).isTrue();
        assertThat(mode.isLectureSeule()).isTrue();
        assertThat(service.definir(true, "crise")).isFalse();
        verify(journal, times(1)).enregistrer(eq(Niveau.WARNING), eq(TypesEvenement.MODE_DEGRADE_ON),
                anyString(), anyString(), anyMap());

        assertThat(service.definir(false, "rétablissement")).isTrue();
        assertThat(mode.isActif()).isFalse();
        verify(journal).enregistrer(eq(Niveau.INFO), eq(TypesEvenement.MODE_DEGRADE_OFF), anyString(),
                anyString(), anyMap());
    }
}
