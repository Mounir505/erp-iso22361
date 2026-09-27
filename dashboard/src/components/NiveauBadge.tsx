/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { AlertOctagon, AlertTriangle, Info } from 'lucide-react'
import type { Niveau } from '@/api/types'
import { Badge } from '@/components/ui/badge'

const CONFIG = {
  INFO: { variant: 'info', icone: Info },
  WARNING: { variant: 'warning', icone: AlertTriangle },
  CRITICAL: { variant: 'critical', icone: AlertOctagon },
} as const

/** Niveau de gravité : toujours icône + libellé, jamais la couleur seule. */
export function NiveauBadge({ niveau }: { niveau: Niveau }) {
  const { variant, icone: Icone } = CONFIG[niveau]
  return (
    <Badge variant={variant}>
      <Icone aria-hidden />
      {niveau}
    </Badge>
  )
}
