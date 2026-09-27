/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState, type FormEvent } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useAdmissions, usePrescriptions } from '@/api/hooks'
import type { Prescription } from '@/api/types'
import { useAuth } from '@/auth/AuthContext'
import { fmtDateHeure } from '@/lib/format'
import { PageHeader } from '@/components/PageHeader'
import { Chargement, EtatVide } from '@/components/EtatVide'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

export function PrescriptionsPage() {
  const { peut } = useAuth()
  const prescriptions = usePrescriptions()
  const [edition, setEdition] = useState<Prescription | 'nouvelle' | null>(null)
  const supprimer = useAction((id: number) => api.delete(`/api/prescriptions/${id}`), {
    succes: 'Prescription supprimée',
    invalider: [['prescriptions']],
  })

  return (
    <>
      <PageHeader
        titre="Prescriptions"
        description="Pharmacie — donnée vitale (risque d’erreur de médication en cas d’indisponibilité)."
        actions={peut('prescriptionsEcriture') && <Button onClick={() => setEdition('nouvelle')}><Plus /> Nouvelle prescription</Button>}
      />
      <Card>
        <CardContent>
          {prescriptions.isPending ? <Chargement /> : prescriptions.data?.length === 0 ? <EtatVide>Aucune prescription.</EtatVide> : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead><TableHead>Patient</TableHead><TableHead>Médicament</TableHead><TableHead>Posologie</TableHead>
                  {peut('prescriptionsEcriture') && <TableHead className="w-20"><span className="sr-only">Actions</span></TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {prescriptions.data?.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="tabular whitespace-nowrap">{fmtDateHeure(p.date)}</TableCell>
                    <TableCell>{p.patientNom}</TableCell>
                    <TableCell className="font-medium">{p.medicament}</TableCell>
                    <TableCell className="text-muted-foreground">{p.posologie}</TableCell>
                    {peut('prescriptionsEcriture') && (
                      <TableCell className="whitespace-nowrap">
                        <Button variant="ghost" size="icon" aria-label="Modifier" onClick={() => setEdition(p)}><Pencil /></Button>
                        <Button variant="ghost" size="icon" aria-label="Supprimer" onClick={() => confirm('Supprimer cette prescription ?') && supprimer.mutate(p.id)}><Trash2 /></Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      {edition && <PrescriptionForm prescription={edition === 'nouvelle' ? undefined : edition} onFermer={() => setEdition(null)} />}
    </>
  )
}

function PrescriptionForm({ prescription, onFermer }: { prescription?: Prescription; onFermer: () => void }) {
  const admissions = useAdmissions()
  const [admissionId, setAdmissionId] = useState(prescription?.admissionId ?? 0)
  const [medicament, setMedicament] = useState(prescription?.medicament ?? '')
  const [posologie, setPosologie] = useState(prescription?.posologie ?? '')
  const enregistrer = useAction(
    () => {
      const corps = { admissionId, medicament: medicament.trim(), posologie: posologie.trim() }
      return prescription ? api.put(`/api/prescriptions/${prescription.id}`, corps) : api.post('/api/prescriptions', corps)
    },
    { succes: 'Prescription enregistrée', invalider: [['prescriptions']], onSuccess: onFermer },
  )
  const enCours = admissions.data?.filter((a) => a.statut === 'EN_COURS' || a.id === prescription?.admissionId)

  function soumettre(e: FormEvent) {
    e.preventDefault()
    enregistrer.mutate(undefined)
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onFermer()}>
      <DialogContent>
        <form onSubmit={soumettre} className="grid gap-4">
          <DialogHeader><DialogTitle>{prescription ? 'Modifier la prescription' : 'Nouvelle prescription'}</DialogTitle></DialogHeader>
          <Field label="Admission en cours" htmlFor="admission">
            <NativeSelect id="admission" value={admissionId} onChange={(e) => setAdmissionId(Number(e.target.value))} disabled={!!prescription}>
              <option value={0} disabled>Choisir…</option>
              {enCours?.map((a) => <option key={a.id} value={a.id}>{a.patientNom} — {a.service}</option>)}
            </NativeSelect>
          </Field>
          <Field label="Médicament" htmlFor="medicament">
            <Input id="medicament" value={medicament} onChange={(e) => setMedicament(e.target.value)} required maxLength={120} />
          </Field>
          <Field label="Posologie" htmlFor="posologie">
            <Input id="posologie" value={posologie} onChange={(e) => setPosologie(e.target.value)} required maxLength={200} />
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onFermer}>Annuler</Button>
            <Button type="submit" disabled={enregistrer.isPending || admissionId === 0}>Enregistrer</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
