/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState, type FormEvent } from 'react'
import type { Patient, StatutPatient } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { LIBELLE_STATUT_PATIENT } from '@/components/StatutBadges'

export interface PatientSaisie { nom: string; dateNaissance: string; statut: StatutPatient }

/** Création / modification d'un patient (données fictives uniquement). */
export function PatientForm({ ouvert, onFermer, initial, onValider, enCours }: {
  ouvert: boolean; onFermer: () => void; initial?: Patient; onValider: (p: PatientSaisie) => void; enCours: boolean
}) {
  const [nom, setNom] = useState(initial?.nom ?? '')
  const [dateNaissance, setDateNaissance] = useState(initial?.dateNaissance ?? '')
  const [statut, setStatut] = useState<StatutPatient>(initial?.statut ?? 'AMBULATOIRE')

  function soumettre(e: FormEvent) {
    e.preventDefault()
    onValider({ nom: nom.trim(), dateNaissance, statut })
  }

  return (
    <Dialog open={ouvert} onOpenChange={(o) => !o && onFermer()}>
      <DialogContent>
        <form onSubmit={soumettre} className="grid gap-4">
          <DialogHeader>
            <DialogTitle>{initial ? 'Modifier le patient' : 'Nouveau patient'}</DialogTitle>
            <DialogDescription>Environnement de formation : saisir uniquement des données fictives.</DialogDescription>
          </DialogHeader>
          <Field label="Nom" htmlFor="nom">
            <Input id="nom" value={nom} onChange={(e) => setNom(e.target.value)} required maxLength={100} />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Date de naissance" htmlFor="ddn">
              <Input id="ddn" type="date" value={dateNaissance} onChange={(e) => setDateNaissance(e.target.value)} required max={new Date().toISOString().slice(0, 10)} />
            </Field>
            <Field label="Statut" htmlFor="statut">
              <NativeSelect id="statut" value={statut} onChange={(e) => setStatut(e.target.value as StatutPatient)}>
                {Object.entries(LIBELLE_STATUT_PATIENT).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </NativeSelect>
            </Field>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onFermer}>Annuler</Button>
            <Button type="submit" disabled={enCours}>{enCours ? 'Enregistrement…' : 'Enregistrer'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
