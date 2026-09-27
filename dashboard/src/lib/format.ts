/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
/** Formatage FR des dates (affichées en heure locale du poste, stockées en UTC). */
const dateHeure = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'medium' })
const heure = new Intl.DateTimeFormat('fr-FR', { timeStyle: 'medium' })
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' })

export const fmtDateHeure = (iso?: string | null) => (iso ? dateHeure.format(new Date(iso)) : '—')
export const fmtHeure = (iso?: string | null) => (iso ? heure.format(new Date(iso)) : '—')
export const fmtDate = (iso?: string | null) => (iso ? date.format(new Date(iso)) : '—')

/** Durée lisible : 42 s · 3 min 05 s · 1 h 02 min. */
export function fmtDuree(secondes?: number | null): string {
  if (secondes == null) return '—'
  const s = Math.max(0, Math.round(secondes))
  if (s < 60) return `${s} s`
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const r = s % 60
  return h > 0 ? `${h} h ${String(m).padStart(2, '0')} min` : `${m} min ${String(r).padStart(2, '0')} s`
}

/** Valeur pour un <input type="datetime-local"> à partir d'un ISO UTC. */
export function versDatetimeLocal(iso?: string | null): string {
  const d = iso ? new Date(iso) : new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export const depuisDatetimeLocal = (v: string) => (v ? new Date(v).toISOString() : undefined)

export function age(dateNaissance: string): number {
  const n = new Date(dateNaissance)
  const now = new Date()
  let a = now.getFullYear() - n.getFullYear()
  if (now < new Date(now.getFullYear(), n.getMonth(), n.getDate())) a--
  return a
}
