/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { Clock, Gauge, Timer, Zap } from 'lucide-react'
import type { Indicateurs } from '@/api/types'
import { fmtDuree, fmtHeure } from '@/lib/format'
import { StatTile } from './StatTile'
import { useMaintenant } from './useMaintenant'

/**
 * Indicateurs de réponse (ISO 22361 5.3.5) : temps de détection, d'activation,
 * de première décision et durée de crise (en direct si la crise est en cours).
 */
export function IndicateursCrise({ ind, titre }: { ind: Indicateurs | null; titre?: string }) {
  const maintenant = useMaintenant()
  const duree = ind == null ? null
    : ind.enCours ? (maintenant - new Date(ind.activation).getTime()) / 1000 : ind.dureeCriseSecondes
  const premiereDecision = ind == null ? null
    : ind.tempsPremiereDecisionSecondes ?? (ind.enCours ? null : undefined)

  return (
    <section aria-label="Indicateurs de réponse">
      {titre && <h2 className="mb-2 text-sm font-medium text-muted-foreground">{titre}</h2>}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile libelle="Temps de détection" icone={<Gauge className="size-4" />}
          valeur={ind ? fmtDuree(ind.tempsDetectionSecondes) : '—'}
          detail={ind ? `1er signal ${fmtHeure(ind.premierSignal)} → seuil ${fmtHeure(ind.declenchement)}` : 'Aucune crise enregistrée'} />
        <StatTile libelle="Temps d’activation" icone={<Zap className="size-4" />}
          valeur={ind ? `${ind.tempsActivationMs} ms` : '—'}
          detail={ind ? `Activation ${ind.typeDeclenchement} à ${fmtHeure(ind.activation)}` : 'Seuil → cellule de crise'} />
        <StatTile libelle="1re décision" icone={<Timer className="size-4" />}
          valeur={premiereDecision == null ? (ind?.enCours ? 'En attente' : '—') : fmtDuree(premiereDecision)}
          detail={ind ? `${ind.nombreDecisions} décision(s) enregistrée(s)` : 'Activation → première décision'} />
        <StatTile libelle="Durée de crise" icone={<Clock className="size-4" />}
          valeur={duree == null ? '—' : fmtDuree(duree)}
          detail={ind ? (ind.enCours ? 'En cours · RTO 4 h' : `Clôturée à ${fmtHeure(ind.cloture)}`) : 'Objectif de reprise (RTO) < 4 h'} />
      </div>
    </section>
  )
}
