/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { BarChart3, ListOrdered } from 'lucide-react'
import { useFluxEvenements, useSituation } from '@/api/hooks'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { DecisionsCard } from './DecisionsCard'
import { EvenementsChart } from './EvenementsChart'
import { ExercicePanel } from './ExercicePanel'
import { IndicateursCrise } from './IndicateursCrise'
import { ModeDegradeCard } from './ModeDegradeCard'
import { SanteCard } from './SanteCard'
import { SeuilsCard } from './SeuilsCard'
import { SituationBanniere } from './SituationBanniere'
import { Timeline } from './Timeline'

/**
 * Tableau de bord de crise — conscience partagée de la situation (ISO 22361 5.3.4.4).
 * Polling conforme au contrat 2.4 : /health toutes les 5 s, /events toutes les 2 s.
 */
export function DashboardCrisePage() {
  const situation = useSituation()
  const flux = useFluxEvenements()
  const s = situation.data
  const evenements = flux.data ?? []

  return (
    <div className="grid gap-5">
      <SituationBanniere situation={s} />
      <IndicateursCrise
        ind={s?.indicateurs ?? s?.derniereCrise ?? null}
        titre={s?.indicateurs ? 'Indicateurs de la crise en cours' : s?.derniereCrise ? 'Indicateurs de la dernière crise' : undefined}
      />

      <div className="grid gap-5 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <div className="grid content-start gap-5">
          <Card>
            <CardHeader>
              <div>
                <CardTitle className="flex items-center gap-2"><BarChart3 className="size-4" /> Événements du journal par minute</CardTitle>
                <CardDescription>30 dernières minutes · {s?.evenementsWarning24h ?? 0} alerte(s) et {s?.evenementsCritical24h ?? 0} franchissement(s) critique(s) sur 24 h</CardDescription>
              </div>
            </CardHeader>
            <CardContent><EvenementsChart evenements={evenements} /></CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2"><ListOrdered className="size-4" /> Timeline des événements</CardTitle>
            </CardHeader>
            <CardContent>
              {flux.isError ? <p className="text-sm text-ink-critical">Flux /events indisponible.</p> : <Timeline evenements={evenements} />}
            </CardContent>
          </Card>
        </div>
        <div className="grid content-start gap-5">
          <SanteCard />
          <DecisionsCard cellule={s?.celluleActive ?? null} />
          <ModeDegradeCard />
          <SeuilsCard />
          <ExercicePanel />
        </div>
      </div>
    </div>
  )
}
