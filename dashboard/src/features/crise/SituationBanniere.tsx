/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { AlertOctagon, AlertTriangle, CheckCircle2, Power, PowerOff } from 'lucide-react'
import { api } from '@/api/client'
import { useAction } from '@/api/hooks'
import type { Situation } from '@/api/types'
import { useAuth } from '@/auth/AuthContext'
import { fmtDateHeure, fmtDuree } from '@/lib/format'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { useMaintenant } from './useMaintenant'

/** Bandeau de situation : NORMAL / ALERTE / CRISE, avec les commandes du pilote. */
export function SituationBanniere({ situation }: { situation: Situation | undefined }) {
  const { peut } = useAuth()
  const maintenant = useMaintenant()
  const invalider = [['situation'], ['cellules'], ['events'], ['decisions']]
  const activer = useAction(() => api.post('/api/crise/cellules/activer', { motif: prompt('Motif de l’activation manuelle ?') ?? null }), {
    succes: 'Cellule de crise activée', invalider,
  })
  const cloturer = useAction(
    (id: number) => api.post(`/api/crise/cellules/${id}/cloturer`, { motif: prompt('Motif de la clôture (sortie de crise) ?') ?? null }),
    { succes: 'Crise clôturée — pensez au REX', invalider },
  )

  const niveau = situation?.niveau ?? 'NORMAL'
  const cellule = situation?.celluleActive
  const conf = {
    NORMAL: { icone: CheckCircle2, titre: 'Situation normale', classe: 'border-level-ok/40 bg-level-ok/8', encre: 'text-ink-ok',
      texte: 'Aucun seuil franchi récemment. Surveillance continue active.' },
    ALERTE: { icone: AlertTriangle, titre: 'Alerte — signal suspect', classe: 'border-level-warning/50 bg-level-warning/10', encre: 'text-ink-warning',
      texte: 'Un seuil d’alerte a été franchi dans les 15 dernières minutes. Équipe technique notifiée.' },
    CRISE: { icone: AlertOctagon, titre: `CRISE — cellule de crise n°${cellule?.id ?? ''} activée`, classe: 'border-level-critical/60 bg-level-critical/10', encre: 'text-ink-critical',
      texte: cellule ? `Activée le ${fmtDateHeure(cellule.dateActivation)} · depuis ${fmtDuree((maintenant - new Date(cellule.dateActivation).getTime()) / 1000)}` : '' },
  }[niveau]
  const Icone = conf.icone

  return (
    <section className={cn('flex flex-wrap items-center justify-between gap-4 rounded-xl border px-5 py-4', conf.classe)} aria-live="polite">
      <div className="flex items-center gap-3">
        <Icone className={cn('size-8 shrink-0', conf.encre)} aria-hidden />
        <div>
          <h1 className={cn('text-lg font-semibold', conf.encre)}>{conf.titre}</h1>
          <p className="text-sm text-muted-foreground">{conf.texte}</p>
        </div>
      </div>
      {peut('pilote') && (
        cellule ? (
          <Button variant="outline" disabled={cloturer.isPending} onClick={() => confirm('Clôturer la crise ? (fin de la phase de réponse)') && cloturer.mutate(cellule.id)}>
            <PowerOff /> Clôturer la crise
          </Button>
        ) : (
          <Button variant="outline" disabled={activer.isPending} onClick={() => confirm('Activer manuellement la cellule de crise ?') && activer.mutate(undefined)}>
            <Power /> Activer la cellule
          </Button>
        )
      )}
    </section>
  )
}
