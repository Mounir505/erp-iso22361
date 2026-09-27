/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import * as React from 'react'
import { Label } from './label'

/** Champ de formulaire : libellé + contrôle + message d'erreur éventuel. */
export function Field({ label, htmlFor, erreur, children }: { label: string; htmlFor: string; erreur?: string; children: React.ReactNode }) {
  return (
    <div className="grid gap-1.5">
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {erreur && <p className="text-xs text-destructive">{erreur}</p>}
    </div>
  )
}
