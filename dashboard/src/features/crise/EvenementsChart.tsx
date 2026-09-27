/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useMemo } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipContentProps } from 'recharts'
import type { NameType, ValueType } from 'recharts/types/component/DefaultTooltipContent'
import type { EvenementJournal, Niveau } from '@/api/types'
import { useMaintenant } from './useMaintenant'

/**
 * Volume d'événements du journal par minute sur les 30 dernières minutes, empilé par niveau.
 * Palette validée CVD (clair + sombre) ; le niveau est aussi donné en texte (légende, info-bulle,
 * timeline) — la couleur n'est jamais le seul porteur d'information.
 */
const NIVEAUX: { cle: Niveau; libelle: string; couleur: string }[] = [
  { cle: 'INFO', libelle: 'Info', couleur: 'var(--level-info)' },
  { cle: 'WARNING', libelle: 'Warning', couleur: 'var(--level-warning)' },
  { cle: 'CRITICAL', libelle: 'Critical', couleur: 'var(--level-critical)' },
]
const MINUTES = 30

type Tranche = { t: number; libelle: string } & Record<Niveau, number>

export function EvenementsChart({ evenements }: { evenements: EvenementJournal[] }) {
  const maintenant = useMaintenant(10_000)
  const donnees = useMemo(() => {
    const fin = Math.floor(maintenant / 60_000) * 60_000
    const debut = fin - (MINUTES - 1) * 60_000
    const tranches: Tranche[] = Array.from({ length: MINUTES }, (_, i) => {
      const t = debut + i * 60_000
      return { t, libelle: new Date(t).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }), INFO: 0, WARNING: 0, CRITICAL: 0 }
    })
    for (const e of evenements) {
      const i = Math.floor((new Date(e.timestamp).getTime() - debut) / 60_000)
      if (i >= 0 && i < MINUTES) tranches[i][e.level]++
    }
    return tranches
  }, [evenements, maintenant])

  return (
    <div>
      <ul className="mb-3 flex flex-wrap gap-4 text-xs text-muted-foreground" aria-label="Légende">
        {NIVEAUX.map((n) => (
          <li key={n.cle} className="flex items-center gap-1.5">
            <span className="size-2.5 rounded-[3px]" style={{ background: n.couleur }} aria-hidden />
            {n.libelle}
          </li>
        ))}
      </ul>
      <div className="h-52">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={donnees} margin={{ top: 4, right: 4, bottom: 0, left: 0 }} barCategoryGap={2}>
            <CartesianGrid vertical={false} stroke="var(--chart-grid)" />
            <XAxis dataKey="libelle" tickLine={false} axisLine={false} interval={4} tick={{ fill: 'var(--muted-foreground)', fontSize: 11 }} />
            <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={36} tick={{ fill: 'var(--muted-foreground)', fontSize: 11 }} />
            <Tooltip cursor={{ fill: 'var(--muted)', opacity: 0.6 }} content={InfoBulle} />
            {NIVEAUX.map((n) => (
              <Bar key={n.cle} dataKey={n.cle} name={n.libelle} stackId="niveaux" fill={n.couleur}
                stroke="var(--card)" strokeWidth={2} shape={SegmentArrondi} isAnimationActive={false} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

/** Segment empilé : seul le segment supérieur non nul d'une colonne a les coins arrondis. */
function SegmentArrondi(props: unknown) {
  const { x, y, width, height, fill, stroke, strokeWidth, payload, dataKey } = props as {
    x: number; y: number; width: number; height: number; fill: string; stroke: string; strokeWidth: number
    payload: Tranche; dataKey: Niveau
  }
  if (!height || height <= 0) return null
  const ordre: Niveau[] = ['INFO', 'WARNING', 'CRITICAL']
  const sommet = [...ordre].reverse().find((k) => payload[k] > 0) === dataKey
  const r = sommet ? Math.min(4, width / 2, height) : 0
  const d = `M${x},${y + height} L${x},${y + r} Q${x},${y} ${x + r},${y} L${x + width - r},${y} Q${x + width},${y} ${x + width},${y + r} L${x + width},${y + height} Z`
  return <path d={d} fill={fill} stroke={stroke} strokeWidth={strokeWidth} />
}

function InfoBulle({ active, payload, label }: TooltipContentProps<ValueType, NameType>) {
  if (!active || !payload?.length) return null
  const total = payload.reduce((s, p) => s + Number(p.value ?? 0), 0)
  return (
    <div className="rounded-md border bg-popover px-3 py-2 text-xs shadow-md">
      <div className="mb-1 font-medium">{label} · {total} événement(s)</div>
      {[...payload].reverse().map((p) => (
        <div key={String(p.dataKey)} className="flex items-center justify-between gap-4">
          <span className="flex items-center gap-1.5 text-muted-foreground">
            <span className="size-2 rounded-[2px]" style={{ background: p.color }} aria-hidden />{p.name}
          </span>
          <span className="tabular font-medium">{p.value}</span>
        </div>
      ))}
    </div>
  )
}
