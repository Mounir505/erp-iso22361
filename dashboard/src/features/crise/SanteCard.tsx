/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useEffect, useState } from 'react'
import { Activity, CheckCircle2, Database, XCircle } from 'lucide-react'
import { useSante } from '@/api/hooks'
import { fmtDuree, fmtHeure } from '@/lib/format'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useMaintenant } from './useMaintenant'

/** État de santé de l'ERP (GET /health toutes les 5 s) ; mesure côté dashboard la durée d'une panne. */
export function SanteCard() {
  const { data, dataUpdatedAt } = useSante()
  const maintenant = useMaintenant()
  const [injoignableDepuis, setInjoignableDepuis] = useState<number | null>(null)
  const up = data?.status === 'UP'

  useEffect(() => {
    if (data === null || data?.status === 'DOWN') setInjoignableDepuis((d) => d ?? Date.now())
    else if (data) setInjoignableDepuis(null)
  }, [data, dataUpdatedAt])

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2"><Activity className="size-4" /> Santé de l’ERP</CardTitle>
        {data === undefined ? <Badge variant="outline">…</Badge> : up
          ? <Badge variant="ok"><CheckCircle2 /> UP</Badge>
          : <Badge variant="critical"><XCircle /> DOWN</Badge>}
      </CardHeader>
      <CardContent className="grid gap-2 text-sm">
        {data === null ? (
          <p className="text-ink-critical">ERP injoignable (conteneur arrêté ou réseau coupé).</p>
        ) : data && (
          <>
            <Ligne libelle={<><Database className="size-3.5" /> Base de données</>} valeur={`${data.database.status} · ${data.database.latencyMs} ms`} />
            <Ligne libelle="Journal en attente" valeur={data.journalEnAttente === 0 ? 'aucun' : `${data.journalEnAttente} entrée(s)`} />
            {data.simulation && <Ligne libelle="Simulation d’exercice" valeur="panne simulée" />}
          </>
        )}
        {injoignableDepuis && (
          <p className="rounded-md bg-level-critical/10 px-2 py-1.5 text-xs text-ink-critical">
            Indisponible depuis {fmtDuree((maintenant - injoignableDepuis) / 1000)} (seuil d’incident : 2 min)
          </p>
        )}
        <p className="text-xs text-muted-foreground">Dernière mesure {dataUpdatedAt ? fmtHeure(new Date(dataUpdatedAt).toISOString()) : '—'} · toutes les 5 s</p>
      </CardContent>
    </Card>
  )
}

function Ligne({ libelle, valeur }: { libelle: React.ReactNode; valeur: string }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="flex items-center gap-1.5 text-muted-foreground">{libelle}</span>
      <span className="tabular font-medium">{valeur}</span>
    </div>
  )
}
