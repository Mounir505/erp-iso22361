/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ArrowLeft, Pencil, Trash2 } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useAdmissions, usePatient } from '@/api/hooks'
import type { Patient } from '@/api/types'
import { useAuth } from '@/auth/AuthContext'
import { age, fmtDate, fmtDateHeure } from '@/lib/format'
import { Chargement, EtatVide } from '@/components/EtatVide'
import { StatutAdmissionBadge, StatutPatientBadge } from '@/components/StatutBadges'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DossierCard } from './DossierCard'
import { PatientForm, type PatientSaisie } from './PatientForm'

export function PatientDetailPage() {
  const id = Number(useParams().id)
  const { peut } = useAuth()
  const navigate = useNavigate()
  const patient = usePatient(id)
  const admissions = useAdmissions(id, peut('admissionsLecture'))
  const [edition, setEdition] = useState(false)

  const modifier = useAction((p: PatientSaisie) => api.put<Patient>(`/api/patients/${id}`, p), {
    succes: 'Patient modifié',
    invalider: [['patient', id], ['patients']],
    onSuccess: () => setEdition(false),
  })
  const supprimer = useAction(() => api.delete(`/api/patients/${id}`), {
    succes: 'Patient supprimé',
    invalider: [['patients']],
    onSuccess: () => navigate('/patients'),
  })

  if (patient.isPending) return <Chargement />
  if (!patient.data) return <EtatVide>Patient introuvable.</EtatVide>
  const p = patient.data

  return (
    <div className="grid gap-5">
      <Link to="/patients" className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Patients
      </Link>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="flex items-center gap-3 text-xl font-semibold">{p.nom} <StatutPatientBadge statut={p.statut} /></h1>
          <p className="mt-1 text-sm text-muted-foreground">Né·e le {fmtDate(p.dateNaissance)} · {age(p.dateNaissance)} ans · dossier n°{p.id}</p>
        </div>
        <div className="flex gap-2">
          {peut('patientsEcriture') && <Button variant="outline" onClick={() => setEdition(true)}><Pencil /> Modifier</Button>}
          {peut('patientsSuppression') && (
            <Button variant="outline" className="text-destructive" disabled={supprimer.isPending}
              onClick={() => confirm(`Supprimer définitivement ${p.nom} et son dossier ?`) && supprimer.mutate(undefined)}>
              <Trash2 /> Supprimer
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-[3fr_2fr]">
        {peut('dossiersLecture') ? <DossierCard patientId={id} /> : (
          <Card><CardContent className="text-sm text-muted-foreground">Le dossier médical est réservé au personnel médical et infirmier.</CardContent></Card>
        )}
        {peut('admissionsLecture') && (
          <Card>
            <CardHeader><CardTitle>Admissions</CardTitle></CardHeader>
            <CardContent>
              {admissions.data?.length === 0 ? <EtatVide>Aucune admission.</EtatVide> : (
                <Table>
                  <TableHeader>
                    <TableRow><TableHead>Entrée</TableHead><TableHead>Service</TableHead><TableHead>Statut</TableHead></TableRow>
                  </TableHeader>
                  <TableBody>
                    {admissions.data?.map((a) => (
                      <TableRow key={a.id}>
                        <TableCell className="tabular">{fmtDateHeure(a.dateEntree)}</TableCell>
                        <TableCell>{a.service}</TableCell>
                        <TableCell><StatutAdmissionBadge statut={a.statut} /></TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        )}
      </div>
      {edition && <PatientForm ouvert initial={p} onFermer={() => setEdition(false)} onValider={(v) => modifier.mutate(v)} enCours={modifier.isPending} />}
    </div>
  )
}
