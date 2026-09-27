/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, jeton, onNonAuthentifie } from '@/api/client'
import type { LoginResponse, Utilisateur } from '@/api/types'
import { reinitialiserFlux } from '@/api/hooks'
import { DROITS, type Droit } from './roles'

interface AuthState {
  utilisateur: Utilisateur | null
  chargement: boolean
  connecter: (login: string, motDePasse: string) => Promise<Utilisateur>
  deconnecter: () => Promise<void>
  peut: (droit: Droit) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

/** Session JWT (sessionStorage : le jeton disparaît à la fermeture de l'onglet). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const [utilisateur, setUtilisateur] = useState<Utilisateur | null>(null)
  const [chargement, setChargement] = useState(() => jeton.lire() != null)

  const oublier = useCallback(() => {
    jeton.ecrire(null)
    setUtilisateur(null)
    reinitialiserFlux()
    qc.clear()
  }, [qc])

  useEffect(() => {
    onNonAuthentifie(oublier)
    if (!jeton.lire()) return
    api.get<Utilisateur>('/api/auth/me')
      .then(setUtilisateur)
      .catch(oublier)
      .finally(() => setChargement(false))
  }, [oublier])

  const connecter = useCallback(async (login: string, motDePasse: string) => {
    const r = await api.post<LoginResponse>('/api/auth/login', { login, motDePasse })
    jeton.ecrire(r.token)
    setUtilisateur(r.utilisateur)
    return r.utilisateur
  }, [])

  const deconnecter = useCallback(async () => {
    try { await api.post('/api/auth/logout') } catch { /* jeton déjà invalide */ }
    oublier()
  }, [oublier])

  const peut = useCallback(
    (droit: Droit) => utilisateur != null && (DROITS[droit] as string[]).includes(utilisateur.role),
    [utilisateur],
  )

  const valeur = useMemo(() => ({ utilisateur, chargement, connecter, deconnecter, peut }), [utilisateur, chargement, connecter, deconnecter, peut])
  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth doit être utilisé dans <AuthProvider>')
  return ctx
}
