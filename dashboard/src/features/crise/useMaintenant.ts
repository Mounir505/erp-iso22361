/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useEffect, useState } from 'react'

/** Horloge locale rafraîchie chaque seconde (durées « en direct »). */
export function useMaintenant(intervalleMs = 1000) {
  const [maintenant, setMaintenant] = useState(() => Date.now())
  useEffect(() => {
    const id = setInterval(() => setMaintenant(Date.now()), intervalleMs)
    return () => clearInterval(id)
  }, [intervalleMs])
  return maintenant
}
