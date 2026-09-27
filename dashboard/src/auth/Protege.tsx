/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { ShieldAlert } from 'lucide-react'
import { useAuth } from './AuthContext'
import type { Droit } from './roles'

/** Garde de route : authentification obligatoire, et droit éventuel. */
export function Protege({ droit, children }: { droit?: Droit; children: ReactNode }) {
  const { utilisateur, chargement, peut } = useAuth()
  const location = useLocation()
  if (chargement) return null
  if (!utilisateur) return <Navigate to="/login" replace state={{ depuis: location.pathname }} />
  if (droit && !peut(droit)) {
    return (
      <div className="mx-auto mt-24 max-w-md text-center">
        <ShieldAlert className="mx-auto size-10 text-muted-foreground" />
        <h2 className="mt-3 text-lg font-semibold">Accès non autorisé</h2>
        <p className="mt-1 text-sm text-muted-foreground">Votre rôle ne donne pas accès à cette page (principe du moindre privilège).</p>
      </div>
    )
  }
  return <>{children}</>
}
