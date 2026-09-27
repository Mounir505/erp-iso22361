/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Pencil, Plus } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useAdmissions, usePatients } from '@/api/hooks'
import type { Admission, StatutAdmission } from '@/api/types'
import { useAuth } from '@/auth/AuthContext'
import { depuisDatetimeLocal, fmtDateHeure, versDatetimeLocal } from '@/lib/format'
import { PageHeader } from '@/components/PageHeader'
import { Chargement, EtatVide } from '@/components/EtatVide'
import { LIBELLE_STATUT_ADMISSION, StatutAdmissionBadge } from '@/components/StatutBadges'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const SERVICES = ['Urgences', 'Cardiologie', 'Chirurgie', 'Médecine interne', 'Pneumologie', 'Pédiatrie', 'Orthopédie', 'Consultations externes']

export function AdmissionsPage() {
  const { peut } = useAuth()
  const admissions = useAdmissions()
  const [edition, setEdition] = useState<Admission | 'nouvelle' | null>(null)

  return (
    <>
      <PageHeader
        titre="Admissions"
        description="Une admission en cours rend le patient « hospitalisé » ; sa clôture le fait sortir."
        actions={peut('admissionsEcriture') && <Button onClick={() => setEdition('nouvelle')}><Plus /> Nouvelle admission</Button>}
      />
      <Card>
        <CardContent>
          {admissions.isPending ? <Chargement /> : admissions.data?.length === 0 ? <EtatVide>Aucune admission.</EtatVide> : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Patient</TableHead><TableHead>Entrée</TableHead><TableHead>Service</TableHead><TableHead>Statut</TableHead>
                  {peut('admissionsEcriture') && <TableHead className="w-10"><span className="sr-only">Actions</span></TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {admissions.data?.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell>{peut('patientsLecture') ? <Link className="font-medium hover:underline" to={`/patients/${a.patientId}`}>{a.patientNom}</Link> : a.patientNom}</TableCell>
                    <TableCell className="tabular">{fmtDateHeure(a.dateEntree)}</TableCell>
                    <TableCell>{a.service}</TableCell>
                    <TableCell><StatutAdmissionBadge statut={a.statut} /></TableCell>
                    {peut('admissionsEcriture') && (
                      <TableCell><Button variant="ghost" size="icon" aria-label="Modifier" onClick={() => setEdition(a)}><Pencil /></Button></TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      {edition && <AdmissionForm admission={edition === 'nouvelle' ? undefined : edition} onFermer={() => setEdition(null)} />}
    </>
  )
}

function AdmissionForm({ admission, onFermer }: { admission?: Admission; onFermer: () => void }) {
  const patients = usePatients('', '')
  const [patientId, setPatientId] = useState(admission?.patientId ?? 0)
  const [dateEntree, setDateEntree] = useState(versDatetimeLocal(admission?.dateEntree))
  const [service, setService] = useState(admission?.service ?? SERVICES[0])
  const [statut, setStatut] = useState<StatutAdmission>(admission?.statut ?? 'EN_COURS')

  const invalider = [['admissions'], ['patients'], ['patient'], ['dossiers-hospitalises']]
  const enregistrer = useAction(
    () => {
      const corps = { patientId, dateEntree: depuisDatetimeLocal(dateEntree), service, statut }
      return admission ? api.put(`/api/admissions/${admission.id}`, corps) : api.post('/api/admissions', corps)
    },
    { succes: admission ? 'Admission modifiée' : 'Admission enregistrée', invalider, onSuccess: onFermer },
  )

  function soumettre(e: FormEvent) {
    e.preventDefault()
    enregistrer.mutate(undefined)
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onFermer()}>
      <DialogContent>
        <form onSubmit={soumettre} className="grid gap-4">
          <DialogHeader><DialogTitle>{admission ? 'Modifier l’admission' : 'Nouvelle admission'}</DialogTitle></DialogHeader>
          <Field label="Patient" htmlFor="patient">
            <NativeSelect id="patient" value={patientId} onChange={(e) => setPatientId(Number(e.target.value))} required disabled={!!admission}>
              <option value={0} disabled>Choisir un patient…</option>
              {patients.data?.map((p) => <option key={p.id} value={p.id}>{p.nom}</option>)}
            </NativeSelect>
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Date d’entrée" htmlFor="entree">
              <Input id="entree" type="datetime-local" value={dateEntree} onChange={(e) => setDateEntree(e.target.value)} required />
            </Field>
            <Field label="Service" htmlFor="service">
              <NativeSelect id="service" value={service} onChange={(e) => setService(e.target.value)}>
                {SERVICES.map((s) => <option key={s}>{s}</option>)}
              </NativeSelect>
            </Field>
          </div>
          <Field label="Statut" htmlFor="statut">
            <NativeSelect id="statut" value={statut} onChange={(e) => setStatut(e.target.value as StatutAdmission)}>
              {Object.entries(LIBELLE_STATUT_ADMISSION).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </NativeSelect>
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onFermer}>Annuler</Button>
            <Button type="submit" disabled={enregistrer.isPending || patientId === 0}>Enregistrer</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
