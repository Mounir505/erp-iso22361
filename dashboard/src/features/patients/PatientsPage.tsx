/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useDeferredValue, useState } from 'react'
import { Link } from 'react-router'
import { Plus, Search } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, usePatients } from '@/api/hooks'
import type { Patient, StatutPatient } from '@/api/types'
import { useAuth } from '@/auth/AuthContext'
import { age, fmtDate } from '@/lib/format'
import { PageHeader } from '@/components/PageHeader'
import { Chargement, EtatVide } from '@/components/EtatVide'
import { LIBELLE_STATUT_PATIENT, StatutPatientBadge } from '@/components/StatutBadges'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PatientForm, type PatientSaisie } from './PatientForm'

export function PatientsPage() {
  const { peut } = useAuth()
  const [q, setQ] = useState('')
  const [statut, setStatut] = useState<StatutPatient | ''>('')
  const recherche = useDeferredValue(q)
  const patients = usePatients(recherche, statut)
  const [creation, setCreation] = useState(false)

  const creer = useAction((p: PatientSaisie) => api.post<Patient>('/api/patients', p), {
    succes: 'Patient créé (dossier médical ouvert)',
    invalider: [['patients']],
    onSuccess: () => setCreation(false),
  })

  return (
    <>
      <PageHeader
        titre="Patients"
        description="Registre des patients (jeu de données entièrement fictif)."
        actions={peut('patientsEcriture') && <Button onClick={() => setCreation(true)}><Plus /> Nouveau patient</Button>}
      />
      <Card>
        <CardContent className="grid gap-4">
          <div className="flex flex-wrap gap-2">
            <div className="relative min-w-56 flex-1">
              <Search className="absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input placeholder="Rechercher par nom…" className="pl-8" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Rechercher" />
            </div>
            <div className="w-48">
              <NativeSelect value={statut} onChange={(e) => setStatut(e.target.value as StatutPatient | '')} aria-label="Filtrer par statut">
                <option value="">Tous les statuts</option>
                {Object.entries(LIBELLE_STATUT_PATIENT).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </NativeSelect>
            </div>
          </div>

          {patients.isPending ? <Chargement /> : patients.data?.length === 0 ? <EtatVide>Aucun patient ne correspond.</EtatVide> : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nom</TableHead>
                  <TableHead>Date de naissance</TableHead>
                  <TableHead className="text-right">Âge</TableHead>
                  <TableHead>Statut</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {patients.data?.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell><Link to={`/patients/${p.id}`} className="font-medium hover:underline">{p.nom}</Link></TableCell>
                    <TableCell>{fmtDate(p.dateNaissance)}</TableCell>
                    <TableCell className="tabular text-right">{age(p.dateNaissance)} ans</TableCell>
                    <TableCell><StatutPatientBadge statut={p.statut} /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      {creation && <PatientForm ouvert onFermer={() => setCreation(false)} onValider={(p) => creer.mutate(p)} enCours={creer.isPending} />}
    </>
  )
}
