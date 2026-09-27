/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState, type FormEvent } from 'react'
import { Gavel, Send } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useDecisions } from '@/api/hooks'
import type { Cellule } from '@/api/types'
import { useAuth } from '@/auth/AuthContext'
import { fmtHeure } from '@/lib/format'
import { EtatVide } from '@/components/EtatVide'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

/** Registre des décisions horodatées de la cellule (ISO 22361 art. 7, traçabilité de la décision). */
export function DecisionsCard({ cellule }: { cellule: Cellule | null }) {
  const { peut, utilisateur } = useAuth()
  const decisions = useDecisions(cellule?.id)
  const [libelle, setLibelle] = useState('')
  const [auteur, setAuteur] = useState('')
  const ajouter = useAction(
    () => api.post(`/api/crise/cellules/${cellule!.id}/decisions`, { libelle, auteur: auteur || null }),
    { succes: 'Décision enregistrée', invalider: [['decisions', cellule?.id], ['situation']], onSuccess: () => setLibelle('') },
  )

  function soumettre(e: FormEvent) {
    e.preventDefault()
    if (libelle.trim()) ajouter.mutate(undefined)
  }

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle className="flex items-center gap-2"><Gavel className="size-4" /> Décisions de la cellule</CardTitle>
          <CardDescription>{cellule ? `Crise n°${cellule.id} — registre horodaté` : 'Aucune cellule active'}</CardDescription>
        </div>
      </CardHeader>
      <CardContent className="grid gap-3">
        {!cellule ? <EtatVide>Les décisions s’enregistrent pendant une crise active.</EtatVide> : (
          <>
            <ol className="grid max-h-72 gap-2 overflow-y-auto">
              {decisions.data?.length === 0 && <li className="text-sm text-muted-foreground">Aucune décision pour l’instant.</li>}
              {decisions.data?.map((d) => (
                <li key={d.id} className="rounded-md border-l-2 border-primary/60 bg-muted/40 px-3 py-2 text-sm">
                  <div>{d.libelle}</div>
                  <div className="mt-0.5 text-xs text-muted-foreground"><span className="tabular">{fmtHeure(d.timestamp)}</span> · {d.auteur}</div>
                </li>
              ))}
            </ol>
            {peut('decisions') && cellule.statut === 'ACTIVE' && (
              <form onSubmit={soumettre} className="grid gap-2 border-t pt-3">
                <Textarea placeholder="Ex. : Isoler le serveur ERP du réseau ; activer la procédure papier aux urgences…" value={libelle}
                  onChange={(e) => setLibelle(e.target.value)} rows={2} maxLength={2000} />
                <div className="flex gap-2">
                  <Input placeholder={`Décideur (défaut : ${utilisateur?.nom})`} value={auteur} onChange={(e) => setAuteur(e.target.value)} maxLength={100} />
                  <Button type="submit" disabled={!libelle.trim() || ajouter.isPending}><Send /> Consigner</Button>
                </div>
              </form>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}
