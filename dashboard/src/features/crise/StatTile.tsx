/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import type { ReactNode } from 'react'
import { Card } from '@/components/ui/card'

/** Indicateur chiffré (« hero number ») : libellé, valeur, précision. */
export function StatTile({ libelle, valeur, detail, icone }: { libelle: string; valeur: ReactNode; detail?: ReactNode; icone?: ReactNode }) {
  return (
    <Card className="gap-1.5 px-5 py-4">
      <div className="flex items-center justify-between text-sm text-muted-foreground">
        {libelle}
        {icone}
      </div>
      <div className="tabular text-2xl font-semibold tracking-tight">{valeur}</div>
      {detail && <div className="text-xs text-muted-foreground">{detail}</div>}
    </Card>
  )
}
