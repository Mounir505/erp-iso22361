/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState } from 'react'
import type { EvenementJournal } from '@/api/types'
import { fmtHeure } from '@/lib/format'
import { NiveauBadge } from '@/components/NiveauBadge'
import { NativeSelect } from '@/components/ui/native-select'

/**
 * Timeline du journal d'audit (flux /events, polling 2 s). Filtre par défaut sur les signaux
 * (WARNING, CRITICAL et actions de la cellule) pour garder la conscience de la situation lisible.
 */
const TYPES_CELLULE = new Set([
  'crisis_decision', 'degraded_mode_on', 'degraded_mode_off', 'crisis_cell_activated', 'crisis_cell_closed',
  'service_restored', 'database_restore', 'database_backup', 'integrity_rebaseline', 'rex_generated',
])

export function Timeline({ evenements }: { evenements: EvenementJournal[] }) {
  const [filtre, setFiltre] = useState<'signaux' | 'tout'>('signaux')
  const lignes = filtre === 'tout' ? evenements : evenements.filter((e) => e.level !== 'INFO' || TYPES_CELLULE.has(e.event_type))

  return (
    <div className="grid gap-3">
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs text-muted-foreground">{lignes.length} ligne(s) · mise à jour toutes les 2 s</span>
        <div className="w-52">
          <NativeSelect value={filtre} onChange={(e) => setFiltre(e.target.value as 'signaux' | 'tout')} aria-label="Filtre de la timeline">
            <option value="signaux">Signaux et actions de crise</option>
            <option value="tout">Tout le journal</option>
          </NativeSelect>
        </div>
      </div>
      <ol className="max-h-[28rem] overflow-y-auto rounded-md border">
        {lignes.length === 0 && <li className="p-4 text-center text-sm text-muted-foreground">Aucun événement.</li>}
        {lignes.map((e) => (
          <li key={e.id ?? e.timestamp} className="grid grid-cols-[auto_1fr] gap-x-3 border-b px-3 py-2 text-sm last:border-0 sm:grid-cols-[5.5rem_6.5rem_1fr]">
            <time className="tabular pt-0.5 text-xs text-muted-foreground" dateTime={e.timestamp}>{fmtHeure(e.timestamp)}</time>
            <div className="sm:order-none"><NiveauBadge niveau={e.level} /></div>
            <div className="col-span-2 min-w-0 sm:col-span-1">
              <div className="break-words">{e.message}</div>
              <div className="mt-0.5 flex flex-wrap gap-x-3 font-mono text-[11px] text-muted-foreground">
                <span>{e.event_type}</span><span>{e.source}</span>{e.user && <span>@{e.user}</span>}
              </div>
            </div>
          </li>
        ))}
      </ol>
    </div>
  )
}
