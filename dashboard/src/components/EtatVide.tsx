/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import type { ReactNode } from 'react'
import { Skeleton } from '@/components/ui/skeleton'

export function Chargement({ lignes = 4 }: { lignes?: number }) {
  return (
    <div className="grid gap-2">
      {Array.from({ length: lignes }, (_, i) => <Skeleton key={i} className="h-9 w-full" />)}
    </div>
  )
}

export function EtatVide({ children }: { children: ReactNode }) {
  return <p className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">{children}</p>
}
