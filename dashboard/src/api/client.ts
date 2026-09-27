/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import type { ApiErrorBody } from './types'

/** Erreur HTTP normalisée (corps ApiError du backend). */
export class ApiError extends Error {
  status: number
  body?: ApiErrorBody
  constructor(status: number, message: string, body?: ApiErrorBody) {
    super(message)
    this.status = status
    this.body = body
  }
}

const CLE_JETON = 'erp.jeton'
let surNonAuthentifie: (() => void) | null = null

export const jeton = {
  lire: () => {
    try { return sessionStorage.getItem(CLE_JETON) } catch { return null }
  },
  ecrire: (t: string | null) => {
    try { t ? sessionStorage.setItem(CLE_JETON, t) : sessionStorage.removeItem(CLE_JETON) } catch { /* stockage indisponible */ }
  },
}

/** Callback appelé sur un 401 (jeton expiré) : le contexte d'auth déconnecte l'utilisateur. */
export function onNonAuthentifie(cb: () => void) {
  surNonAuthentifie = cb
}

async function requete(methode: string, chemin: string, corps?: unknown): Promise<Response> {
  const entetes: Record<string, string> = {}
  const t = jeton.lire()
  if (t) entetes.Authorization = `Bearer ${t}`
  if (corps !== undefined) entetes['Content-Type'] = 'application/json'
  const res = await fetch(chemin, { method: methode, headers: entetes, body: corps === undefined ? undefined : JSON.stringify(corps) })
  if (!res.ok) {
    let body: ApiErrorBody | undefined
    try { body = await res.json() } catch { /* corps non JSON */ }
    if (res.status === 401 && t && !chemin.startsWith('/api/auth/login')) surNonAuthentifie?.()
    throw new ApiError(res.status, body?.message ?? `Erreur HTTP ${res.status}`, body)
  }
  return res
}

async function json<T>(methode: string, chemin: string, corps?: unknown): Promise<T> {
  const res = await requete(methode, chemin, corps)
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  get: <T>(chemin: string) => json<T>('GET', chemin),
  post: <T>(chemin: string, corps?: unknown) => json<T>('POST', chemin, corps ?? {}),
  put: <T>(chemin: string, corps: unknown) => json<T>('PUT', chemin, corps),
  delete: (chemin: string) => json<void>('DELETE', chemin),
  /** Téléchargement d'un fichier (export REX). */
  telecharger: async (chemin: string, corps: unknown, nomParDefaut: string) => {
    const res = await requete('POST', chemin, corps)
    const blob = await res.blob()
    const dispo = res.headers.get('Content-Disposition') ?? ''
    const nom = /filename="?([^";]+)"?/.exec(dispo)?.[1] ?? nomParDefaut
    const url = URL.createObjectURL(blob)
    const a = Object.assign(document.createElement('a'), { href: url, download: nom })
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  },
}

/** Message lisible pour un toast. */
export function messageErreur(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.body?.champs) return `${e.message} : ${Object.entries(e.body.champs).map(([k, v]) => `${k} ${v}`).join(', ')}`
    return e.message
  }
  return e instanceof Error ? e.message : 'Erreur inattendue'
}
